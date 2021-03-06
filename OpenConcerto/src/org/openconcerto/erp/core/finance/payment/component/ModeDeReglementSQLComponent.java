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
 
 package org.openconcerto.erp.core.finance.payment.component;

import org.openconcerto.erp.config.Log;
import org.openconcerto.erp.core.common.element.BanqueSQLElement;
import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.model.BanqueModifiedListener;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.sqlobject.SQLSearchableTextCombo;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ITextCombo;
import org.openconcerto.utils.CollectionUtils;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.EventListenerList;

public class ModeDeReglementSQLComponent extends BaseSQLComponent {

    static private enum Mode {
        ECHEANCE, CHEQUE, VIREMENT
    }

    static public final int MONTH_END = 31;
    static public final int AT_INVOICE_DATE = 0;

    private final ElementComboBox boxBanque = new ElementComboBox(true, 20);
    private final JPanel panelBanque = new JPanel(new GridBagLayout());
    private final JPanel panelEcheance = new JPanel(new GridBagLayout());
    private final EventListenerList banqueModifiedListenerList = new EventListenerList();
    private final SQLRequestComboBox comboTypeReglement = new SQLRequestComboBox();
    private ITextCombo comboA;
    private final ITextCombo comboLe = new SQLTextCombo();
    private final SQLSearchableTextCombo comboBanque = new SQLSearchableTextCombo();
    private final JRadioButton buttonFinMois = new JRadioButton("fin de mois");
    private final JRadioButton buttonDateFacture = new JRadioButton("date de facturation");
    private final JRadioButton buttonLe = new JRadioButton("le");
    private final JCheckBox checkboxComptant = new JCheckBox("Comptant");
    private final JDate dateDepot = new JDate(false);
    private final JDate dateVirt = new JDate(false);
    private final JDate dateCheque = new JDate(false);
    private final JTextField numeroChq = new JTextField();
    private final JTextField nom = new JTextField(30);
    private JPanel panelActive = null;
    private final Map<Mode, JPanel> m = new HashMap<Mode, JPanel>();

    private int rowIdMode;

    private void setComponentModeEnabled(final SQLRow rowTypeRegl) {

        if (rowTypeRegl != null && this.rowIdMode != rowTypeRegl.getID()) {
            this.rowIdMode = rowTypeRegl.getID();
            System.err.println("ModeDeReglementNGSQLComponent.setComponentModeEnabled() " + this.rowIdMode);
        } else {

            return;
        }

        final ButtonGroup group = new ButtonGroup();
        group.add(this.buttonFinMois);
        group.add(this.buttonDateFacture);
        group.add(this.buttonLe);

        final Boolean forceLater = rowTypeRegl.getBoolean("ECHEANCE");
        final Boolean forceNow = rowTypeRegl.getBoolean("COMPTANT");
        if (forceLater && forceNow)
            Log.get().warning("Force opposite for " + rowTypeRegl);

        this.allowEditable(this.getView(this.checkboxComptant), !forceLater && !forceNow);

        if (forceLater) {
            this.checkboxComptant.setSelected(false);
        }
        if (forceNow) {
            this.checkboxComptant.setSelected(true);
        }

        updatePanel();
    }

    public ModeDeReglementSQLComponent(final SQLElement elt) {
        super(elt);
    }

    // private
    private final JPanel infosCheque = new JPanel(new GridBagLayout());
    private final JPanel infosVirt = new JPanel(new GridBagLayout());

    @Override
    protected Set<String> createRequiredNames() {
        return CollectionUtils.createSet("ID_TYPE_REGLEMENT");
    }

    @Override
    public void addViews() {

        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;

        /*******************************************************************************************
         * SELECTION DU MODE DE REGLEMENT
         ******************************************************************************************/
        this.comboA = new SQLTextCombo(false);

        final GridBagConstraints cB = new DefaultGridBagConstraints();
        this.panelBanque.setOpaque(false);
        this.panelBanque.add(new JLabel(getLabelFor("ID_" + BanqueSQLElement.TABLENAME)), cB);
        cB.weightx = 1;
        cB.gridx++;
        this.panelBanque.add(this.boxBanque, cB);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(this.panelBanque, c);

        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridy++;
        c.gridheight = 1;
        this.add(new JLabel("Règlement par"), c);

        this.comboTypeReglement.setPreferredSize(new Dimension(80, new JTextField().getPreferredSize().height));
        DefaultGridBagConstraints.lockMinimumSize(this.comboTypeReglement);
        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(this.comboTypeReglement, c);
        c.gridheight = 1;
        // Mode de règlement
        c.gridx++;
        DefaultGridBagConstraints.lockMinimumSize(this.checkboxComptant);
        this.checkboxComptant.setOpaque(false);
        this.add(this.checkboxComptant, c);

        // Infos sur le reglement, depend du type de reglement et du comptant oui/non
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridheight = GridBagConstraints.REMAINDER;
        createPanelChequeComptant();
        this.add(this.infosCheque, c);
        createPanelVirementComptant();
        this.add(this.infosVirt, c);
        createPanelEcheance();
        this.add(this.panelEcheance, c);

        this.addView(this.comboTypeReglement, "ID_TYPE_REGLEMENT");
        this.addView(this.checkboxComptant, "COMPTANT");

        // cheque
        this.addSQLObject(this.comboBanque, "ETS");
        this.addSQLObject(this.numeroChq, "NUMERO");
        this.addSQLObject(this.dateCheque, "DATE");
        this.addSQLObject(this.dateDepot, "DATE_DEPOT");

        // virement
        this.addSQLObject(this.nom, "NOM");
        this.addSQLObject(this.dateVirt, "DATE_VIREMENT");

        this.addRequiredSQLObject(this.comboA, "AJOURS");
        this.addSQLObject(this.buttonDateFacture, "DATE_FACTURE");
        this.addSQLObject(this.buttonFinMois, "FIN_MOIS");
        this.addRequiredSQLObject(this.comboLe, "LENJOUR");

        // Listeners

        this.addSQLObject(this.boxBanque, "ID_" + BanqueSQLElement.TABLENAME);
        this.boxBanque.setButtonsVisible(false);
        this.boxBanque.setOpaque(false);
        this.boxBanque.addModelListener("wantedID", new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                final Integer i = ModeDeReglementSQLComponent.this.boxBanque.getWantedID();
                final int value = (i == null) ? -1 : Integer.valueOf(i);
                fireBanqueIdChange(value);
            }
        });

        this.comboTypeReglement.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                final Integer id = ModeDeReglementSQLComponent.this.comboTypeReglement.getValue();
                if (id != null && id > 1) {

                    final SQLRow ligneTypeReg = SQLBackgroundTableCache.getInstance().getCacheForTable(getTable().getBase().getTable("TYPE_REGLEMENT")).getRowFromId(id);
                    setComponentModeEnabled(ligneTypeReg);
                }
            }
        });

        this.buttonLe.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                allowEditable(getView(ModeDeReglementSQLComponent.this.comboLe), e.getStateChange() == ItemEvent.SELECTED && !ModeDeReglementSQLComponent.this.checkboxComptant.isSelected());
            }
        });
        // initial value
        this.allowEditable(this.getView(this.comboLe), false);

        this.buttonFinMois.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                // System.err.println("Fin de mois");
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ModeDeReglementSQLComponent.this.comboLe.setValue(String.valueOf(MONTH_END));
                }
            }
        });

        this.buttonDateFacture.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                // System.err.println("Date de facturation");
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ModeDeReglementSQLComponent.this.comboLe.setValue(String.valueOf(AT_INVOICE_DATE));
                }
            }
        });

        this.getView(this.comboLe).addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final Number newVal = (Number) evt.getNewValue();
                if (newVal != null && newVal.intValue() != AT_INVOICE_DATE && newVal.intValue() != MONTH_END) {
                    ModeDeReglementSQLComponent.this.buttonLe.setSelected(true);
                }
            }
        });

        this.checkboxComptant.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                setEcheanceEnabled(e.getStateChange() == ItemEvent.DESELECTED);
            }
        });
    }

    private void createPanelEcheance() {

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        this.panelEcheance.setOpaque(false);
        this.panelEcheance.add(new JLabel("A"), c);
        c.gridx++;
        c.gridwidth = 1;
        this.comboA.setMinimumSize(new Dimension(60, this.comboA.getMinimumSize().height));
        this.comboA.setPreferredSize(new Dimension(60, this.comboA.getMinimumSize().height));
        this.comboA.setMaximumSize(new Dimension(60, this.comboA.getMinimumSize().height));
        this.panelEcheance.add(this.comboA, c);
        c.gridx += 1;
        c.gridwidth = 1;
        this.panelEcheance.add(new JLabel("jours,"), c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.buttonDateFacture.setOpaque(false);
        this.panelEcheance.add(this.buttonDateFacture, c);
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        this.buttonFinMois.setOpaque(false);
        this.panelEcheance.add(this.buttonFinMois, c);
        c.gridy++;
        c.gridwidth = 1;
        this.buttonLe.setOpaque(false);
        this.panelEcheance.add(this.buttonLe, c);

        c.gridx++;
        this.comboLe.setMinimumSize(new Dimension(60, this.comboLe.getMinimumSize().height));
        this.comboLe.setPreferredSize(new Dimension(60, this.comboLe.getMinimumSize().height));
        this.comboLe.setMaximumSize(new Dimension(60, this.comboLe.getMinimumSize().height));
        this.panelEcheance.add(this.comboLe, c);
        this.panelActive = this.panelEcheance;
        this.m.put(Mode.ECHEANCE, this.panelEcheance);

        DefaultGridBagConstraints.lockMinimumSize(this.panelEcheance);

    }

    public void createPanelChequeComptant() {
        // this.infosCheque.setBorder(BorderFactory.createTitledBorder("Informations Chèque"));
        final GridBagConstraints cCheque = new DefaultGridBagConstraints();
        this.infosCheque.add(new JLabel("Banque"), cCheque);
        cCheque.gridx++;

        this.infosCheque.add(this.comboBanque, cCheque);
        cCheque.gridx++;
        this.infosCheque.add(new JLabel("N°"), cCheque);
        cCheque.gridx++;
        DefaultGridBagConstraints.lockMinimumSize(this.numeroChq);
        this.infosCheque.add(this.numeroChq, cCheque);
        cCheque.gridy++;
        cCheque.gridx = 0;
        this.infosCheque.add(new JLabel("Daté du"), cCheque);
        cCheque.gridx++;

        this.infosCheque.add(this.dateCheque, cCheque);

        final JLabel labelDepot = new JLabel("A déposer après le");
        cCheque.gridx++;
        DefaultGridBagConstraints.lockMinimumSize(this.infosCheque);
        this.infosCheque.add(labelDepot, cCheque);

        cCheque.gridx++;
        this.infosCheque.add(this.dateDepot, cCheque);
        this.m.put(Mode.CHEQUE, this.infosCheque);
        this.infosCheque.setVisible(false);
        this.infosCheque.setOpaque(false);
        DefaultGridBagConstraints.lockMinimumSize(this.infosCheque);
    }

    public void createPanelVirementComptant() {
        // this.infosVirt.setBorder(BorderFactory.createTitledBorder("Informations Virement"));
        final GridBagConstraints cCheque = new DefaultGridBagConstraints();
        cCheque.weightx = 1;
        this.infosVirt.add(new JLabel("Libellé"), cCheque);
        cCheque.gridx++;

        this.infosVirt.add(this.nom, cCheque);
        cCheque.gridy++;
        cCheque.gridx = 0;
        cCheque.fill = GridBagConstraints.NONE;
        cCheque.weightx = 0;
        this.infosVirt.add(new JLabel("Daté du"), cCheque);
        cCheque.gridx++;

        this.infosVirt.add(this.dateVirt, cCheque);
        this.m.put(Mode.VIREMENT, this.infosVirt);
        this.infosVirt.setVisible(false);
        DefaultGridBagConstraints.lockMinimumSize(this.infosVirt);
    }

    private void updatePanel() {
        final Integer typeReglt = this.comboTypeReglement.getValue();
        if (typeReglt == null)
            return;
        final boolean comptant = this.checkboxComptant.isSelected();

        final Mode mode;
        if (comptant && typeReglt == TypeReglementSQLElement.CHEQUE) {
            mode = Mode.CHEQUE;
        } else if (comptant && typeReglt == TypeReglementSQLElement.TRAITE) {
            mode = Mode.VIREMENT;
        } else {
            mode = Mode.ECHEANCE;
        }
        replacePanel(mode);
    }

    private void replacePanel(final Mode mode) {
        final JPanel panel = this.m.get(mode);
        if (panel != this.panelActive) {
            // System.err.println("replace panel " + mode);
            this.panelActive.setVisible(false);
            panel.setVisible(true);
            this.panelActive = panel;
        }
    }

    private void fireBanqueIdChange(final int id) {
        final BanqueModifiedListener[] l = this.banqueModifiedListenerList.getListeners(BanqueModifiedListener.class);
        for (final BanqueModifiedListener banqueModifiedListener : l) {
            banqueModifiedListener.idChange(id);
        }
    }

    // Active/Desactive le panel pour specifie la date d'echeance
    private void setEcheanceEnabled(final boolean b) {
        // System.err.println("set echeance to " + b);
        this.allowEditable(this.getView(this.comboA), b);
        this.allowEditable(this.getView(this.comboLe), b && this.buttonLe.isSelected());
        this.allowEditable(this.getView(this.buttonFinMois), b);
        this.allowEditable(this.getView(this.buttonDateFacture), b);
        this.buttonLe.setEnabled(b);
        if (!b) {
            this.comboA.setValue("0");
            this.buttonDateFacture.setSelected(true);
        } else {
            // TODO factor with createDefaults()
            this.comboA.setValue("30");
            this.buttonFinMois.setSelected(true);
        }

        updatePanel();
    }

    // ATTN sometimes overwritten by ModeReglementDefautPrefPanel.getDefaultRow(true);
    @Override
    protected SQLRowValues createDefaults() {
        final SQLRowValues vals = new SQLRowValues(getTable());
        vals.put("COMPTANT", Boolean.FALSE);
        vals.put("AJOURS", 30);
        vals.put("FIN_MOIS", Boolean.TRUE);
        return vals;
    }

    public void setWhereBanque(final Where w) {
        if (this.boxBanque != null && this.boxBanque.isShowing()) {
            final ComboSQLRequest request = this.boxBanque.getRequest();
            if (request != null) {
                request.setWhere(w);
                this.boxBanque.fillCombo();
            }
        }
    }

    public void setSelectedIdBanque(final int id) {
        this.boxBanque.setValue(id);
    }

    public int getSelectedIdBanque() {
        return this.boxBanque.getSelectedId();
    }

    public void addBanqueModifiedListener(final BanqueModifiedListener e) {
        this.banqueModifiedListenerList.add(BanqueModifiedListener.class, e);
    }
}
