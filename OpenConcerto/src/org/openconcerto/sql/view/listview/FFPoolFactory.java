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
 
 package org.openconcerto.sql.view.listview;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.DatabaseGraph;

import java.util.ArrayList;
import java.util.List;

// ID_ARTICLE_1,_2,_3 or ID_OBSERVATION,_2,_3
public abstract class FFPoolFactory extends ItemPoolFactory {

    private final SQLTable t;
    private final SQLTable foreignT;
    private final List<SQLField> fields;

    public FFPoolFactory(SQLTable t, String foreignT) {
        this(t, foreignT, null);
    }

    public FFPoolFactory(SQLTable t, String foreignT, int count, boolean firstSuffixed) {
        this(t, foreignT, computeFF(foreignT, count, firstSuffixed));
    }

    public FFPoolFactory(SQLTable t, String foreignT, List<String> fields) {
        this.t = t;
        this.foreignT = t.getDBRoot().getTable(foreignT);

        this.fields = new ArrayList<SQLField>();
        final DatabaseGraph g = this.t.getDBSystemRoot().getGraph();
        if (fields == null)
            fields = DatabaseGraph.getNames(g.getForeignLinks(this.t, this.foreignT), new ArrayList<String>());
        for (final String fieldName : fields) {
            final SQLField f = this.t.getField(fieldName);
            if (this.foreignT.equals(g.getForeignTable(f))) {
                this.fields.add(f);
            } else
                throw new IllegalArgumentException(f + " does not refer to " + foreignT);

        }
    }

    protected final List<SQLField> getFields() {
        return this.fields;
    }

    protected final SQLTable getForeignTable() {
        return this.foreignT;
    }

    protected final SQLTable getTable() {
        return this.t;
    }

    public abstract ItemPool create(ListSQLView panel);

    public final List<SQLRowAccessor> getItems(SQLRowAccessor r) {
        final List<SQLRowAccessor> res = new ArrayList<SQLRowAccessor>();
        for (final SQLField f : this.getFields()) {
            if (!r.isForeignEmpty(f.getName()))
                res.add(r.getForeign(f.getName()));
        }
        return res;
    }

    public String toString() {
        return this.getClass() + " on " + this.getFields();
    }
}
