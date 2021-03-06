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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.DeviseCellEditor;
import org.openconcerto.erp.core.common.ui.MultiLineTableCellEditor;
import org.openconcerto.erp.core.common.ui.RowValuesMultiLineEditTable;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ITextWithCompletion;
import org.openconcerto.sql.view.list.AutoCompletionManager;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableControlPanel;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.sql.view.list.TextTableCellEditorWithCompletion;
import org.openconcerto.sql.view.list.ValidStateChecker;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.checks.ValidState;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class SaisieKmItemTable extends JPanel implements MouseListener {

    private final RowValuesTable table;
    private final SQLTableElement debit;
    private final SQLTableElement credit;
    private final SQLTableElement tableElementNumeroCompte;
    private final CompteRowValuesRenderer numeroCompteRenderer = new CompteRowValuesRenderer();
    private final DeviseKmRowValuesRenderer deviseRenderer = new DeviseKmRowValuesRenderer();
    private final DeviseCellEditor deviseCellEditor = new DeviseCellEditor();

    public SaisieKmItemTable(final SQLRowValues defaultRowVals) {
        setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;

        final SQLElement elt = Configuration.getInstance().getDirectory().getElement("SAISIE_KM_ELEMENT");

        // TODO Obligation de choisir un compte correct
        final List<SQLTableElement> list = new Vector<SQLTableElement>();
        final SQLTable tableElement = elt.getTable();
        this.tableElementNumeroCompte = new SQLTableElement(tableElement.getField("NUMERO"));
        list.add(this.tableElementNumeroCompte);

        final SQLTableElement tableElementNomCompte = new SQLTableElement(tableElement.getField("NOM"));
        list.add(tableElementNomCompte);

        if (tableElement.getTable().contains("NOM_PIECE")) {
            final SQLTableElement tableElementNomPiece = new SQLTableElement(tableElement.getField("NOM_PIECE"));
            list.add(tableElementNomPiece);
        }

        final SQLTableElement tableElementNomEcriture = new SQLTableElement(tableElement.getField("NOM_ECRITURE"));
        list.add(tableElementNomEcriture);

        //

        this.debit = new SQLTableElement(tableElement.getField("DEBIT"), Long.class, this.deviseCellEditor);
        list.add(this.debit);
        this.credit = new SQLTableElement(tableElement.getField("CREDIT"), Long.class, this.deviseCellEditor);
        list.add(this.credit);
        if (!DefaultNXProps.getInstance().getBooleanValue("HideAnalytique")) {
            final AnalytiqueItemTable analytiqueAssocTable = new AnalytiqueItemTable(true);
            SQLTableElement eltPourcentAnalytique = new SQLTableElement(tableElement.getField("ANALYTIQUE"), String.class,
                    new MultiLineTableCellEditor((RowValuesMultiLineEditTable) analytiqueAssocTable.getTable(), analytiqueAssocTable));
            list.add(eltPourcentAnalytique);
        }

        final RowValuesTableModel model = new RowValuesTableModel(elt, list, tableElement.getField("NUMERO"), false, defaultRowVals) {
            @Override
            public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
                super.setValueAt(aValue, rowIndex, columnIndex);

                final int debitIndex = getColumnIndexForElement(SaisieKmItemTable.this.debit);
                final int creditIndex = getColumnIndexForElement(SaisieKmItemTable.this.credit);

                // float debitVal = ((Float) model.getValueAt(rowIndex, debitIndex);

                if (debitIndex == columnIndex && ((Long) aValue).longValue() != 0 && ((Long) getValueAt(rowIndex, creditIndex)).longValue() != 0) {
                    setValueAt(Long.valueOf(0), rowIndex, creditIndex);
                } else {
                    if (creditIndex == columnIndex && ((Long) aValue).longValue() != 0 && ((Long) getValueAt(rowIndex, debitIndex)).longValue() != 0) {
                        setValueAt(Long.valueOf(0), rowIndex, debitIndex);
                    }
                }
            }
        };

        this.table = new RowValuesTable(model, null);
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

        // Autocompletion
        final AutoCompletionManager m = new AutoCompletionManager(this.tableElementNumeroCompte,
                ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("COMPTE_PCE.NUMERO"), this.table, this.table.getRowValuesTableModel(),
                ITextWithCompletion.MODE_STARTWITH, true, false, new ValidStateChecker() {

                    ComptePCESQLElement elt = Configuration.getInstance().getDirectory().getElement(ComptePCESQLElement.class);

                    @Override
                    public ValidState getValidState(Object o) {
                        if (o != null) {
                            return elt.getCompteNumeroValidState(o.toString());
                        }
                        return super.getValidState(o);
                    }
                });
        m.fill("NOM", "NOM");
        m.setFillWithField("NUMERO");
        final Where w = new Where(elt.getTable().getTable("COMPTE_PCE").getField("OBSOLETE"), "=", Boolean.FALSE);
        m.setWhere(w);

        // FIXME erreur fill numero
        final AutoCompletionManager m2 = new AutoCompletionManager(tableElementNomCompte, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("COMPTE_PCE.NOM"),
                this.table, this.table.getRowValuesTableModel(), ITextWithCompletion.MODE_CONTAINS, true);
        m2.fill("NUMERO", "NUMERO");
        m2.setFillWithField("NOM");
        m2.setWhere(w);

        TextTableCellEditorWithCompletion t = (TextTableCellEditorWithCompletion) this.tableElementNumeroCompte.getTableCellEditor(this.table);

        this.add(new RowValuesTableControlPanel(this.table), c);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        this.add(new JScrollPane(this.table), c);

        this.tableElementNumeroCompte.setRenderer(this.numeroCompteRenderer);
        this.debit.setRenderer(this.deviseRenderer);
        this.credit.setRenderer(this.deviseRenderer);

        this.table.addMouseListener(this);
        this.table.getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                // Sélectionne automatiquement la ligne ajoutée
                if (e.getType() == TableModelEvent.INSERT) {
                    editCellAt(e.getFirstRow(), 0);
                }

            }
        });

    }

    /**
     * Charge une ecriture dans une ligne de la RowValuesTable
     * 
     * @param idEcriture id de l'ecriture à charger
     * @param contrePasser contrePasser l'ecriture
     */
    private void loadEcriture(final SQLRow ecrRow, final boolean contrePasser) {
        assert SwingUtilities.isEventDispatchThread();
        final SQLRow compteRow = ecrRow.getForeignRow("ID_COMPTE_PCE");
        final Map<String, Object> m = new HashMap<String, Object>();

        m.put("NUMERO", compteRow.getString("NUMERO"));
        m.put("NOM", compteRow.getString("NOM"));

        if (ecrRow.getTable().contains("NOM_PIECE")) {
            m.put("NOM_PIECE", ecrRow.getString("NOM_PIECE"));
        }

        if (contrePasser) {
            m.put("NOM_ECRITURE", "Contrepassation - " + ecrRow.getString("NOM"));
            m.put("DEBIT", ecrRow.getObject("CREDIT"));
            m.put("CREDIT", ecrRow.getObject("DEBIT"));
        } else {
            m.put("NOM_ECRITURE", ecrRow.getString("NOM"));
            m.put("DEBIT", ecrRow.getObject("DEBIT"));
            m.put("CREDIT", ecrRow.getObject("CREDIT"));
        }

        final SQLRowValues rowVals = new SQLRowValues(getModel().getSQLElement().getTable(), m);
        this.table.getRowValuesTableModel().addRow(rowVals, false);

    }

    /**
     * Remplit la RowValuesTable avec les ecritures du mouvement
     * 
     * @param idMvt id du mouvement
     * @param contrePasser contrePasser le mouvement
     */
    public void loadMouvement(final int idMvt, final boolean contrePasser) {
        assert SwingUtilities.isEventDispatchThread();
        // FIXME load analytique
        final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        final SQLTable ecrTable = base.getTable("ECRITURE");

        final SQLSelect selEcriture = new SQLSelect();
        selEcriture.addSelectStar(ecrTable);

        final Where w = new Where(ecrTable.getField("ID_MOUVEMENT"), "=", idMvt);
        selEcriture.setWhere(w);

        final String reqEcriture = selEcriture.asString();

        final List<SQLRow> myListEcriture = (List<SQLRow>) base.getDataSource().execute(reqEcriture, SQLRowListRSH.createFromSelect(selEcriture, ecrTable));

        this.table.getRowValuesTableModel().clearRows();

        for (SQLRow sqlRow : myListEcriture) {
            loadEcriture(sqlRow, contrePasser);
        }
        this.table.getRowValuesTableModel().fireTableDataChanged();
    }

    public void updateField(final String field, final int id) {
        this.table.updateField(field, id);
    }

    public void insertFrom(final String field, final int id) {
        this.table.insertFrom(field, id);
    }

    public void insertFrom(final SQLRowAccessor row) {
        this.table.insertFrom(row);
    }

    public RowValuesTableModel getModel() {
        return this.table.getRowValuesTableModel();
    }

    public SQLTableElement getCreditElement() {
        return this.credit;
    }

    public SQLTableElement getDebitElement() {
        return this.debit;
    }

    public SQLTableElement getNumeroCompteElement() {
        return this.tableElementNumeroCompte;
    }

    public void setCreateAutoActive(final boolean b) {
        this.numeroCompteRenderer.setCreateActive(b);
        this.table.revalidate();
        this.table.repaint();
    }

    public void setRowDeviseValidAt(final boolean b, final int index) {
        this.deviseRenderer.setValid(b, index);

    }

    public void editCellAt(final int row, final int column) {
        assert SwingUtilities.isEventDispatchThread();
        this.table.setColumnSelectionInterval(column, column);
        this.table.setRowSelectionInterval(row, row);
        this.table.editCellAt(row, column);
    }

    private long getContrepartie() {
        assert SwingUtilities.isEventDispatchThread();
        long totalCred = 0;
        long totalDeb = 0;
        final RowValuesTableModel model = this.table.getRowValuesTableModel();
        final int creditIndex = model.getColumnIndexForElement(getCreditElement());
        final int debitIndex = model.getColumnIndexForElement(getDebitElement());
        for (int i = 0; i < this.table.getRowCount(); i++) {
            if (model.isRowValid(i)) {
                final Long fTc = (Long) model.getValueAt(i, creditIndex);
                if (fTc != null) {
                    totalCred += fTc.longValue();
                }
                final Long fTd = (Long) model.getValueAt(i, debitIndex);
                if (fTd != null) {
                    totalDeb += fTd.longValue();
                }
            }
        }
        return totalDeb - totalCred;
    }

    private long getSoldeRow(final int index) {
        assert SwingUtilities.isEventDispatchThread();
        if (index >= 0 && index < this.table.getRowCount()) {
            final SQLRowValues rowVals = this.table.getRowValuesTableModel().getRowValuesAt(index);
            return rowVals.getLong("DEBIT") - rowVals.getLong("CREDIT");
        } else {
            return 0;
        }
    }

    public void fillEmptyEntryLabel(String previousText, String text) {
        assert SwingUtilities.isEventDispatchThread();
        if (text == null)
            return;
        RowValuesTableModel model = table.getRowValuesTableModel();
        int size = model.getRowCount();
        for (int i = 0; i < size; i++) {
            SQLRowValues r = model.getRowValuesAt(i);
            if (r.getString("NOM_ECRITURE") == null || r.getString("NOM_ECRITURE").trim().isEmpty() || r.getString("NOM_ECRITURE").trim().equals(previousText)) {
                r.put("NOM_ECRITURE", text);
            }
        }
        model.fireTableDataChanged();
    }

    public void mousePressed(final MouseEvent e) {
        final int rowSel = this.table.getSelectedRow();
        if (e.getButton() == MouseEvent.BUTTON3 && rowSel >= 0 && rowSel < this.table.getRowCount()) {
            final JPopupMenu menuDroit = new JPopupMenu();

            menuDroit.add(new AbstractAction("Contrepartie") {
                public void actionPerformed(final ActionEvent ev) {

                    long l = getContrepartie();
                    if (SaisieKmItemTable.this.table.getRowValuesTableModel().isRowValid(rowSel)) {
                        l += getSoldeRow(rowSel);
                    }

                    if (l > 0) {
                        SaisieKmItemTable.this.table.getRowValuesTableModel().putValue(Long.valueOf(0), rowSel, "DEBIT");
                        SaisieKmItemTable.this.table.getRowValuesTableModel().putValue(l, rowSel, "CREDIT");
                    } else {
                        SaisieKmItemTable.this.table.getRowValuesTableModel().putValue(Long.valueOf(0), rowSel, "CREDIT");
                        SaisieKmItemTable.this.table.getRowValuesTableModel().putValue(-l, rowSel, "DEBIT");
                    }
                }
            });
            menuDroit.pack();
            menuDroit.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
            menuDroit.setVisible(true);
        }
    }

    public void mouseReleased(final MouseEvent e) {
    }

    public void mouseClicked(final MouseEvent e) {
    }

    public void mouseEntered(final MouseEvent e) {
    }

    public void mouseExited(final MouseEvent e) {
    }

}
