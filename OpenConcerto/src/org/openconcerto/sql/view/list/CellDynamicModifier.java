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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.model.SQLRowValues;

import java.util.IdentityHashMap;
import java.util.Map;

abstract public class CellDynamicModifier {

    private final Map<SQLRowValues, Object> values = new IdentityHashMap<SQLRowValues, Object>();

    public void setValueFrom(final SQLRowValues row, final Object value) {
        values.put(row, value);
    }

    public Object getValueFrom(final SQLRowValues row, SQLTableElement source) {
        if (!values.containsKey(row)) {
            setValueFrom(row, computeValueFrom(row, source));
        }
        return values.get(row);
    }

    public void clear() {
        this.values.clear();
    }

    public abstract Object computeValueFrom(final SQLRowValues row, SQLTableElement source);

}
