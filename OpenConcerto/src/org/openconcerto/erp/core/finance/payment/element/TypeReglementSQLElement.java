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
 
 package org.openconcerto.erp.core.finance.payment.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class TypeReglementSQLElement extends ComptaSQLConfElement {

    public static final int CHEQUE = 2;
    public static final int CB = 3;
    public static final int ESPECE = 4;
    public static final int TRAITE = 5;
    public static final int INDEFINI = 6;
    public static final int VIREMENT = 7;
    public static final int CESU = 8;

    public TypeReglementSQLElement() {
        super("TYPE_REGLEMENT", "Type de règlement", "Type de règlement");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("ID_COMPTE_PCE_CLIENT");
        l.add("ID_COMPTE_PCE_FOURN");
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
        return new BaseSQLComponent(this) {
            private JTextField textNom;

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // libellé
                JLabel labelNom = new JLabel("Libellé");
                this.add(labelNom, c);

                c.gridx++;
                c.weightx = 1;
                this.textNom = new JTextField();
                this.add(this.textNom, c);

                // Choix compte client
                final ElementComboBox numeroCompteClient = new ElementComboBox();
                JLabel labelCompteCli = new JLabel("Compte Client");
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                labelCompteCli.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelCompteCli, c);

                c.gridx++;
                this.add(numeroCompteClient, c);

                // Choix compte
                final ElementComboBox numeroCompteFourn = new ElementComboBox();
                JLabel labelCompteFourn = new JLabel("Compte Fournisseur");
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                labelCompteFourn.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelCompteFourn, c);

                c.gridx++;
                this.add(numeroCompteFourn, c);

                this.addView(this.textNom, "NOM");
                this.addView(numeroCompteClient, "ID_COMPTE_PCE_CLIENT", REQ);
                this.addView(numeroCompteFourn, "ID_COMPTE_PCE_FOURN", REQ);
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".type";
    }
}
