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
 
 package org.openconcerto.sql.view;

import org.openconcerto.sql.TM;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLComponent.Mode;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.rights.TableAllRights;
import org.openconcerto.sql.users.rights.UserRights;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.doc.Documented;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.border.Border;

/**
 * @author ilm Created on 8 oct. 2003
 */
public class EditPanel extends JPanel implements IListener, ActionListener, Documented {

    public static enum EditMode {
        CREATION {
            @Override
            protected Mode getCompMode() {
                return Mode.INSERTION;
            }
        },
        MODIFICATION {
            @Override
            protected Mode getCompMode() {
                return Mode.MODIFICATION;
            }
        },
        READONLY {
            @Override
            protected Mode getCompMode() {
                return Mode.READ_ONLY;
            }
        };
        protected abstract Mode getCompMode();
    };

    /**
     * If this system property is true, then new lines are always added at the end of the list.
     * Otherwise they're added after the selection.
     */
    public final static String ADD_AT_THE_END = "org.openconcerto.sql.editPanel.endAdd";
    /**
     * If this system property is true, then the scroll pane won't have a border.
     */
    public static final String NOBORDER = "org.openconcerto.editpanel.noborder";
    /**
     * If this system property is true, add a separator before the buttons.
     */
    public static final String ADD_SEPARATOR = "org.openconcerto.editpanel.separator";

    public final static EditMode CREATION = EditMode.CREATION;
    public final static EditMode MODIFICATION = EditMode.MODIFICATION;
    public final static EditMode READONLY = EditMode.READONLY;

    private final EditMode mode;
    private JButton jButtonSupprimer;
    private JButton jButtonModifier;
    private JButton jButtonAjouter;
    private JButton jButtonAnnuler;
    private final SQLComponent component;

    private JCheckBox keepOpen = new JCheckBox(TM.tr("editPanel.keepOpen"));

    private final List<EditPanelListener> panelListeners = new Vector<EditPanelListener>();// EditPanelListener
    private JScrollPane p;
    private final SQLElement element;
    private IListe l;
    // whether our component is valid
    private ValidState valid = ValidState.getNoReasonInstance(false);

    /**
     * Creates an creation panel
     * 
     * @param e the element to display.
     */
    public EditPanel(SQLElement e) {
        this(e, CREATION);
    }

    public EditPanel(SQLElement e, EditMode mode) {
        this(e.createDefaultComponent(), mode);
    }

    public EditPanel(SQLComponent e, EditMode mode) {
        this(e, mode, Collections.<SQLField> emptyList());
    }

    public EditPanel(SQLElement e, EditMode mode, List<SQLField> hiddenFields) {
        this(e.createDefaultComponent(), mode, hiddenFields);
    }

    /**
     * Creates an instance:
     * <ul>
     * <li>to create if mode is CREATION</li>
     * <li>to modify if MODIFICATION</li>
     * <li>to view if READONLY</li>
     * </ul>
     * use {@value #NOBORDER} and {@value #ADD_SEPARATOR} for custom style
     * 
     * @param e the element to display.
     * @param mode the edit mode, one of CREATION, MODIFICATION or READONLY.
     * @param hiddenFields
     */
    public EditPanel(SQLComponent e, EditMode mode, List<SQLField> hiddenFields) {
        super();

        this.l = null;

        // ATTN verification seulement dans uiInit()
        this.mode = mode;
        this.element = e.getElement();

        this.component = e;
        try {
            this.component.setMode(mode.getCompMode());
            this.component.setNonExistantEditable(this.getMode() == CREATION);
            if (this.component instanceof BaseSQLComponent) {
                for (int i = 0; i < hiddenFields.size(); i++) {
                    final SQLField hiddenField = hiddenFields.get(i);
                    ((BaseSQLComponent) this.component).doNotShow(hiddenField);
                }
            }

            if (this.getMode() != READONLY) {
                // on écoute les changements de validation,
                // avant component.uiInit() car il fait un fireValidChange()
                this.component.addValidListener(new ValidListener() {
                    @Override
                    public void validChange(ValidObject src, ValidState newValue) {
                        // expensive so cache it
                        EditPanel.this.valid = newValue;
                        updateBtns();
                    }
                });
                final PropertyChangeListener updateBtnsListener = new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateBtns();
                    }
                };
                // update buttons if the selection changes of row or if the current row changes
                ((BaseSQLComponent) this.component).addSelectionListener(updateBtnsListener);
                this.getSQLComponent().addPropertyChangeListener(SQLComponent.READ_ONLY_PROP, updateBtnsListener);
            }

            this.uiInit();
            this.component.uiInit();

            if (Boolean.getBoolean(NOBORDER)) {
                // don't use null, it will be replaced by updateUI()
                this.setInnerBorder(BorderFactory.createEmptyBorder());
            }
        } catch (Exception ex) {
            ExceptionHandler.handle(TM.tr("init.error"), ex);
        }
    }

    public final EditMode getMode() {
        return this.mode;
    }

    private void updateBtns() {
        updateBtn(this.jButtonAjouter, true, false, "noRightToAdd", TableAllRights.ADD_ROW_TABLE);
        updateBtn(this.jButtonModifier, true, true, "noRightToModify", TableAllRights.MODIFY_ROW_TABLE);
        updateBtn(this.jButtonSupprimer, false, true, "noRightToDel", TableAllRights.DELETE_ROW_TABLE);
    }

    private void updateBtn(final JButton b, final boolean needValid, final boolean needID, final String desc, final String code) {
        if (b != null) {
            final ValidState res;
            final boolean idOK = this.getSQLComponent().getSelectedID() >= SQLRow.MIN_VALID_ID;
            final UserRights rights = UserRightsManager.getCurrentUserRights();
            if (!TableAllRights.hasRight(rights, code, getSQLComponent().getElement().getTable())) {
                res = ValidState.createCached(false, TM.tr(desc));
            } else if (this.getSQLComponent().isSelectionReadOnly()) {
                res = ValidState.createCached(false, TM.tr("editPanel.readOnlySelection"));
            } else if (needID && !idOK) {
                res = ValidState.createCached(false, TM.tr("editPanel.inexistentElement"));
            } else if (needValid && !this.valid.isValid()) {
                res = this.valid;
            } else {
                res = ValidState.getTrueInstance();
            }
            updateBtn(b, res);
        }
    }

    private final void uiInit() {
        this.fill();

        // les raccourcis claviers
        this.component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "apply");
        this.component.getActionMap().put("apply", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                apply();
            }
        });
    }

    protected void fill() {
        Container container = this;

        container.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 1;
        c.weightx = 0;
        c.insets = new Insets(2, 2, 1, 2);
        // container.add(new JLabel(this.getTitle()), c);

        c.weighty = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;

        this.p = new JScrollPane(this.component);
        this.p.getVerticalScrollBar().setUnitIncrement(9);
        this.p.setOpaque(false);
        this.p.getViewport().setOpaque(false);
        this.p.setViewportBorder(null);
        this.p.setMinimumSize(new Dimension(60, 60));

        container.add(this.p, c);

        // Separator, if needed
        if (Boolean.getBoolean(ADD_SEPARATOR)) {
            c.gridy++;
            c.weightx = 1;
            c.weighty = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.BOTH;
            final JSeparator comp = new JSeparator();
            // Evite les tremblements verticaux
            comp.setMinimumSize(new Dimension(comp.getPreferredSize()));
            container.add(comp, c);
        }

        // Buttons
        c.gridy++;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.NONE;
        this.keepOpen.setOpaque(false);
        if (this.getMode() == CREATION) {

            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.EAST;
            if (!Boolean.getBoolean("org.openconcerto.editpanel.hideKeepOpen")) {
                container.add(this.keepOpen, c);
            }
            c.fill = GridBagConstraints.NONE;
            c.gridx = 2;
            c.anchor = GridBagConstraints.EAST;
            this.jButtonAjouter = new JButton(TM.tr("add"));
            container.add(this.jButtonAjouter, c);
            // Listeners
            this.jButtonAjouter.addActionListener(this);
            this.jButtonAjouter.addMouseListener(new MouseAdapter() {
                // not mouseClicked() since we'd be called after mouse release, i.e. after the
                // action. In an EditFrame (with "do not close" checked) on an IListPanel when add
                // is performed the list will select the new row, in doing so it first clears the
                // selection, which empties us, then we get called and thus display that all
                // required fields are empty
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!jButtonAjouter.isEnabled()) {
                        final String toolTipText = jButtonAjouter.getToolTipText();
                        if (toolTipText != null && !toolTipText.isEmpty()) {
                            JOptionPane.showMessageDialog(EditPanel.this, toolTipText);
                        }
                    }
                }
            });
        } else if (this.getMode() == MODIFICATION) {
            c.gridx = 1;
            c.anchor = GridBagConstraints.EAST;
            this.jButtonModifier = new JButton(TM.tr("saveModifications"));
            container.add(this.jButtonModifier, c);
            c.weightx = 0;
            c.gridx = 2;
            this.jButtonSupprimer = new JButton(TM.tr("remove"));
            container.add(this.jButtonSupprimer, c);
            // Listeners
            this.jButtonModifier.addActionListener(this);
            this.jButtonSupprimer.addActionListener(this);
        }
        c.weightx = 0;
        c.gridx = 3;
        c.anchor = GridBagConstraints.EAST;
        if (this.getMode() == READONLY)
            this.jButtonAnnuler = new JButton(TM.tr("close"));
        else
            this.jButtonAnnuler = new JButton(TM.tr("cancel"));
        container.add(this.jButtonAnnuler, c);
        // Listeners
        this.jButtonAnnuler.addActionListener(this);
        // this.getContentPane().add(container);
        // this.getContentPane().add(new JScrollPane(container));
    }

    /**
     * Redimensionne la frame pour qu'elle soit de taille maximum sans déborder de l'écran.
     * <img src="doc-files/resizeFrame.png"/>
     */
    public Dimension getViewResizedDimesion(Dimension frameSize) {

        // MAYBE remonter la frame pour pas qu'elle dépasse en bas
        final Dimension viewSize = this.p.getViewport().getView().getSize();
        final int verticalHidden = viewSize.height - this.p.getVerticalScrollBar().getVisibleAmount();
        final int horizontalHidden = viewSize.width - this.p.getHorizontalScrollBar().getVisibleAmount();

        final Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        final int maxV = ((int) bounds.getMaxY()) - this.getY();
        final int maxH = ((int) bounds.getMaxX()) - this.getX();

        final int vertical = Math.min(frameSize.height + verticalHidden, maxV);
        final int horizontal = Math.min(frameSize.width + horizontalHidden, maxH);

        return new Dimension(horizontal, vertical);

    }

    public void setInnerBorder(Border b) {
        this.p.setBorder(b);
    }

    public void selectionId(int id, int field) {
        // inutile de ne se remplir qu'avec des valeurs valides (pour éviter le resetValue() qd
        // déselection et donc l'écrasement des modif en cours) car de toute façon on est obligé de
        // laisser passer les valides qui écrasent tout autant.
        if (id < SQLRow.MIN_VALID_ID)
            this.component.select(null);
        else if (this.getMode() == CREATION) {
            this.component.select(this.element.createCopy(id));
        } else {
            this.component.select(id);
        }
    }

    protected final void apply() {
        final JButton b;
        if (this.getMode() == CREATION)
            b = this.jButtonAjouter;
        else if (this.getMode() == MODIFICATION)
            b = this.jButtonModifier;
        else if (this.getMode() == READONLY)
            b = this.jButtonAnnuler;
        else
            b = null;

        if (b != null)
            b.doClick();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == this.jButtonAnnuler) {
            try {
                this.fireCancelled();// this.dispose();
            } catch (Throwable ex) {
                ExceptionHandler.handle(TM.tr("editPanel.cancelError"), ex);
            }
        } else if (e.getSource() == this.jButtonModifier) {
            try {
                modifier();
            } catch (Throwable ex) {
                ExceptionHandler.handle(TM.tr("editPanel.modifyError"), ex);
            }
        } else if (e.getSource() == this.jButtonAjouter) {
            try {
                ajouter();
            } catch (Throwable ex) {
                ExceptionHandler.handle(TM.tr("editPanel.addError"), ex);
            }
        } else if (e.getSource() == this.jButtonSupprimer) {
            try {
                if (this.element.askArchive(this, this.component.getSelectedID())) {
                    this.fireDeleted();
                    // this.dispose(); // on ferme la fenetre
                }
            } catch (Throwable ex) {
                ExceptionHandler.handle(TM.tr("editPanel.deleteError"), ex);
            }
        }
    }

    public void modifier() {
        this.component.update();
        this.fireModified();
    }

    public boolean alwaysVisible() {
        return this.keepOpen.isSelected();
    }

    public void setAlwaysVisible(boolean b) {
        this.keepOpen.setSelected(true);
    }

    private void ajouter() {
        // ne pas laisser ajouter par le raccourci clavier quand le bouton est grisé
        if (this.jButtonAjouter.isEnabled()) {
            final int id;
            if (!Boolean.getBoolean(ADD_AT_THE_END) && this.getIListe() != null && !this.getIListe().isDead() && this.getIListe().getDesiredRow() != null)
                id = this.component.insert(this.getIListe().getDesiredRow());
            else
                id = this.component.insert();
            if (this.getIListe() != null)
                this.getIListe().selectID(id);
            // otherwise full reset on visibility change.
            if (this.alwaysVisible()) {
                ((BaseSQLComponent) this.getSQLComponent()).partialReset();
            }
            this.fireInserted(id);
        }
    }

    private void fireDeleted() {
        for (int i = 0; i < this.panelListeners.size(); i++) {
            EditPanelListener listener = this.panelListeners.get(i);
            listener.deleted();
        }
    }

    private void fireInserted(int id) {
        for (int i = 0; i < this.panelListeners.size(); i++) {
            EditPanelListener listener = this.panelListeners.get(i);
            listener.inserted(id);
        }
    }

    private void fireModified() {
        for (int i = 0; i < this.panelListeners.size(); i++) {
            EditPanelListener listener = this.panelListeners.get(i);
            listener.modified();
        }
    }

    private void fireCancelled() {
        for (int i = 0; i < this.panelListeners.size(); i++) {
            EditPanelListener listener = this.panelListeners.get(i);
            listener.cancelled();
        }
    }

    public void addEditPanelListener(EditPanelListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("null listener");
        }
        if (!this.panelListeners.contains(listener)) {
            this.panelListeners.add(listener);
        }
    }

    public void removeEditPanelListener(EditPanelListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("null listener");
        }
        if (this.panelListeners.contains(listener)) {
            this.panelListeners.remove(listener);
        }
    }

    static void updateBtn(JButton btn, ValidState validState) {
        btn.setEnabled(validState.isValid());
        btn.setToolTipText(computeTooltip(validState));
    }

    static String computeTooltip(ValidState validState) {
        return computeTooltip(validState.isValid(), validState.getValidationText());
    }

    static private String computeTooltip(boolean valid, final String cause) {
        final String res;
        if (valid)
            res = null;
        else {
            final String c = cause == null ? "" : cause.trim();
            String validationText = TM.tr("editPanel.invalidContent");
            if (c.length() > 0)
                validationText += "\n" + c;
            else
                validationText += TM.tr("editPanel.invalidContent.unknownReason");
            res = "<html>" + validationText.replace("\n", "<br>") + "</html>";
        }
        return res;
    }

    public void disableCancel() {
        this.jButtonAnnuler.setVisible(false);
    }

    public void disableDelete() {
        if (this.jButtonSupprimer != null)
            this.jButtonSupprimer.setVisible(false);
    }

    public void resetValue() {
        this.component.resetValue();
    }

    public void addComponentListenerOnViewPort(ComponentListener l) {
        this.p.getViewport().getView().addComponentListener(l);
    }

    public void setModifyLabel(String label) {
        this.jButtonModifier.setText(label);

    }

    public SQLComponent getSQLComponent() {
        return this.component;
    }

    /**
     * Permet de forcer qq valeur
     */
    public void setValues(List<SQLRow> sqlRows) {
        // eg /BATIMENT/
        final SQLTable t = this.component.getElement().getTable();
        final SQLRowValues vals = new SQLRowValues(t);
        // eg |BATIMENT.ID_SITE|
        final SQLField parentFF = this.component.getElement().getParentForeignField();
        if (parentFF == null)
            return;
        // eg /SITE/
        final SQLTable foreignT = parentFF.getForeignTable();

        for (int i = 0; i < sqlRows.size(); i++) {
            final SQLRow row = sqlRows.get(i);
            if (row.getTable().equals(foreignT)) {
                vals.put(parentFF.getName(), row.getID());
            }
        }

        this.component.select(vals);
    }

    public final void setIListe(IListe l) {
        this.l = l;
    }

    private final IListe getIListe() {
        return this.l;
    }

    public String getDocId() {
        return this.getMode() + "_" + this.element.getTable().getName();
    }

    public String getGenericDoc() {
        return "";
    }

    public boolean onScreen() {
        return false;
    }

    public boolean isDocTransversable() {
        return true;
    }

}
