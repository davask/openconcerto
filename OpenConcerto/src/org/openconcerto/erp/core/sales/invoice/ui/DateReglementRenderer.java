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
 
 package org.openconcerto.erp.core.sales.invoice.ui;

import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.sql.view.list.ListSQLLine;
import org.openconcerto.ui.table.TableCellRendererUtils;

import java.awt.Color;
import java.awt.Component;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class DateReglementRenderer extends DefaultTableCellRenderer {
    private static final Color orange = new Color(249, 215, 176);
    private static final Color orangeDark = orange.darker();
    private static final Color blue = new Color(153, 215, 251);
    private static final Color blueDark = blue.darker();
    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.FRENCH);

    public DateReglementRenderer() {
        super();
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        ListSQLLine line = ITableModel.getLine(table.getModel(), row);
        Long l = line.getRow().getLong("T_AVOIR_TTC");
        if (l == null || l == 0) {
            if (value == null) {
                if (isSelected) {
                    comp.setBackground(orangeDark);
                } else {
                    comp.setBackground(orange);
                }
            } else {
                TableCellRendererUtils.setBackgroundColor(comp, table, isSelected);
            }

        } else {
            if (isSelected) {
                comp.setBackground(blueDark);
            } else {
                comp.setBackground(blue);
            }
        }

        if (value instanceof Date) {
            this.setText(dateFormat.format((Date) value));
        }
        return comp;
    }
}
