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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.erp.config.Log;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.StringUtils;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

public class DeviseNumericHTConvertorCellEditor extends DeviseNumericWithRangeCellEditor implements MouseListener {

    private float taxe = TaxeCache.getCache().getFirstTaxe().getFloat("TAUX");

    public DeviseNumericHTConvertorCellEditor(SQLField field) {
        super(field);
        this.textField.addMouseListener(this);
        setMin(null);
    }

    public void setTaxe(float d) {
        this.taxe = d;
    }

    public void mousePressed(MouseEvent e) {
        if (textField.getText().trim().length() > 0 && e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu menuDroit = new JPopupMenu();
            menuDroit.add(new AbstractAction("Convertir en HT (TVA " + taxe + ")") {

                public void actionPerformed(ActionEvent e) {
                    final BigDecimal prixTTC = StringUtils.getBigDecimalFromUserText(textField.getText());
                    if (prixTTC != null) {
                        try {
                            BigDecimal taux = new BigDecimal(taxe).movePointLeft(2).add(BigDecimal.ONE);
                            BigDecimal divide = prixTTC.divide(taux, DecimalUtils.HIGH_PRECISION);
                            divide = divide.setScale(precision, RoundingMode.HALF_UP);
                            textField.setText(divide.toString());
                        } catch (Exception ex) {
                            Log.get().info("Cannot substract tax from " + prixTTC + " for tax " + taxe);
                            ex.printStackTrace();
                        }
                    }
                }
            });
            menuDroit.pack();
            menuDroit.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
            menuDroit.setVisible(true);
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
