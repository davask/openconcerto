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
 
 package org.openconcerto.sql.sqlobject.itemview;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.MutableRowItemView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A mutable RIV handling fields, sqlName and label.
 * 
 * @author Sylvain
 * @see RowItemViewComponent
 */
public abstract class BaseRowItemView implements MutableRowItemView {

    static public final <T> T getOnlyOne(final List<T> l) {
        if (l.size() == 0)
            return null;
        else if (l.size() > 1)
            throw new IllegalStateException("More than one : " + l);
        else
            return l.get(0);
    }

    private final List<SQLField> fields;

    private String sqlName;

    public BaseRowItemView() {
        this.fields = new ArrayList<SQLField>();
    }

    @Override
    public final void init(String sqlName, Set<SQLField> fields) {
        this.sqlName = sqlName;
        this.fields.addAll(fields);

        this.init();

        if (this.getComp() instanceof RowItemViewComponent)
            ((RowItemViewComponent) this.getComp()).init(this);
    }

    protected abstract void init();

    @Override
    public final List<SQLField> getFields() {
        return this.fields;
    }

    @Override
    public final SQLField getField() {
        return getOnlyOne(this.fields);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " on " + this.getFields();
    }

    // *** by default, insert is the same as update
    @Override
    public void insert(SQLRowValues vals) {
        this.update(vals);
    }

    @Override
    public final String getSQLName() {
        return this.sqlName;
    }
}
