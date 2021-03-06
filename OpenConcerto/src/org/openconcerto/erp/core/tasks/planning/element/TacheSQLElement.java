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
 
 package org.openconcerto.erp.core.tasks.planning.element;

import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

public class TacheSQLElement extends ConfSQLElement {

    public TacheSQLElement() {
        super("TACHE_COMMON", "une tache", "taches");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("FAIT");
        l.add("PRIORITE");
        l.add("NOM");
        // l.add("ID_RESSOURCE");
        l.add("DATE");
        l.add("DATE_FIN");
        l.add("DATE_FAIT");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new UISQLComponent(this) {
            public void addViews() {
                this.addRequiredSQLObject(new JTextField(), "NOM", "left");
                this.addRequiredSQLObject(new JTextField(), "DATE", "right");
            }
        };
    }

}
