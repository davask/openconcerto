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
 
 /*
 * Créé le 2 mai 2005
 */
package org.openconcerto.sql.element;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.MutableRowItemView;
import org.openconcerto.sql.sqlobject.itemview.BaseRowItemView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;

/**
 * @author Sylvain CUAZ
 */
public abstract class BaseSQLObject extends JPanel implements MutableRowItemView {

    private List<SQLField> fields;
    private String sqlName;

    public BaseSQLObject() {
        super();
        this.setOpaque(false);
    }

    @Override
    public void init(String sqlName, Set<SQLField> fields) {
        this.sqlName = sqlName;
        this.fields = new ArrayList<SQLField>(fields);
    }

    @Override
    public final String getSQLName() {
        return this.sqlName;
    }

    @Override
    public final SQLField getField() {
        return BaseRowItemView.getOnlyOne(this.fields);
    }

    @Override
    public List<SQLField> getFields() {
        return this.fields;
    }

    public SQLTable getTable() {
        return this.getField().getTable();
    }
}
