/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.sql.changer.correct;

import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;

import java.util.EnumSet;

public class FixSerial extends Changer<SQLTable> {

    public FixSerial(DBSystemRoot b) {
        super(b);
    }

    protected EnumSet<SQLSystem> getCompatibleSystems() {
        // not needed on H2, see Column.updateSequenceIfRequired()
        return EnumSet.of(SQLSystem.POSTGRESQL);
    }

    /**
     * Set the current value of the sequence for the primary key to the max of the primary key.
     * Useful after importing rows without using the default value for the primary key (thus not
     * updating the sequence).
     * 
     * @param t the table to fix.
     */
    protected void changeImpl(SQLTable t) {
        getStream().print(t + "... ");
        if (!t.isRowable()) {
            getStream().println("not rowable");
        } else {
            final SQLName seqName = getPrimaryKeySeq(t);
            if (seqName != null) {
                final SQLSelect sel = new SQLSelect(true);
                sel.addSelect(t.getKey(), "max");
                final Number maxID = (Number) this.getDS().executeScalar(sel.asString());
                // begin at 1 if table is empty
                final long nextID = maxID == null ? 1 : maxID.longValue() + 1;
                // for some reason this doesn't always work (maybe a cache pb ?):
                // final String s = "SELECT setval('" + seqName + "', " + maxID + ")";
                // while this does
                final String s = "ALTER SEQUENCE " + seqName.quote() + " RESTART WITH " + nextID;
                this.getDS().executeScalar(s);
                getStream().println("done");
            } else
                getStream().println("no sequence: " + t.getKey().getDefaultValue());
        }
    }

    static public SQLName getPrimaryKeySeq(SQLTable t) throws IllegalStateException {
        if (!t.isRowable()) {
            return null;
        } else {
            final SQLField key = t.getKey();
            if (key == null) {
                return null;
            } else {
                return key.getOwnedSequence();
            }
        }
    }
}
