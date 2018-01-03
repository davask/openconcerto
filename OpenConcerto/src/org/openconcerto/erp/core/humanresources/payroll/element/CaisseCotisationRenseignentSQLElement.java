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
 
 package org.openconcerto.erp.core.humanresources.payroll.element;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.utils.ListMap;

public class CaisseCotisationRenseignentSQLElement extends ConfSQLElement {

    public CaisseCotisationRenseignentSQLElement() {
        super("CAISSE_COTISATION_RENSEIGNEMENT", "une caisse renseignement", "caisses renseignements");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("IDENTIFIANT");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("IDENTIFIANT");
        return l;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        return ListMap.singleton(null, "IDENTIFIANT");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            public void addViews() {

                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Nom
                JLabel labelNom = new JLabel(getLabelFor("NOM"));
                JTextField textNom = new JTextField();
                this.add(labelNom, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textNom, c);
                c.weightx = 0;

                this.addSQLObject(textNom, "NOM");
            }
        };
    }
}