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
 
 package org.openconcerto.erp.panel;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

public class ITreeSelectionNode extends DefaultMutableTreeNode {

    private SQLRowAccessor row;

    public ITreeSelectionNode(SQLRowAccessor row) {
        this.row = row;
    }

    public String toString() {

        if (this.row == null) {
            return "";
        } else {
            List<SQLField> fields = Configuration.getInstance().getShowAs().getFieldExpand(this.row.getTable());
            if (fields == null) {
                throw new IllegalStateException("no field to expand for table " + this.row.getTable());
            }
            StringBuffer result = new StringBuffer();
            for (SQLField s : fields) {
                if (this.row.getObject(s.getName()) != null) {
                    result.append(this.row.getObject(s.getName()).toString() + " ");
                }
            }
            return result.toString().trim();
        }
    }

    public int getId() {
        return this.row == null ? 1 : this.row.getID();
    }

    public void setRow(SQLRow row) {
        this.row = row;
    }
}
