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
 
 package org.openconcerto.sql.sqlobject;

import org.openconcerto.sql.element.RIVPanel;
import org.openconcerto.sql.element.SQLComponentItem;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.request.ComboSQLRequest.KeepMode;
import org.openconcerto.sql.request.SQLForeignRowItemView;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.view.list.ITableModel.SleepState;
import org.openconcerto.sql.view.search.SearchSpec;
import org.openconcerto.ui.FontUtils;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.InteractionMode;
import org.openconcerto.ui.component.InteractionMode.InteractionComponent;
import org.openconcerto.ui.component.combo.ISearchableCombo;
import org.openconcerto.ui.component.combo.SearchMode;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.coreanimation.Pulseable;
import org.openconcerto.ui.valuewrapper.ValueChangeSupport;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.checks.EmptyChangeSupport;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.model.DefaultIMutableListModel;
import org.openconcerto.utils.model.NewSelection;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.accessibility.Accessible;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.JTextComponent;

/**
 * A comboBox who lists items provided by a ComboSQLRequest. It listens to table changes, but can
 * also be reloaded by calling {@link #fillCombo()}. Search is also available , see
 * {@link #search(SearchSpec)}.
 * 
 * @author Sylvain CUAZ
 * @see #uiInit(ComboSQLRequest)
 */
public class SQLRequestComboBox extends JPanel implements SQLForeignRowItemView, ValueWrapper<Integer>, EmptyObj, TextComponent, Pulseable, SQLComponentItem, InteractionComponent {

    public static final String UNDEFINED_STRING = "----- ??? -----";

    // on l'a pas toujours à l'instanciation
    private IComboModel req;

    // Interface graphique
    protected final ISearchableCombo<IComboSelectionItem> combo;

    // supports
    private final ValueChangeSupport<Integer> supp;
    private final EmptyChangeSupport emptySupp;

    // le mode actuel
    private InteractionMode mode;
    // le mode à sélectionner à la fin du updateAll
    // null when not used
    private InteractionMode modeToSelect;

    // to speed up the combo
    private final String stringStuff;

    public SQLRequestComboBox() {
        this(true);
    }

    public SQLRequestComboBox(boolean addUndefined) {
        this(addUndefined, -1);
    }

    public SQLRequestComboBox(boolean addUndefined, int preferredWidthInChar) {
        this.setOpaque(false);
        this.mode = InteractionMode.READ_WRITE;
        this.modeToSelect = null;
        if (preferredWidthInChar > 0) {
            final char[] a = new char[preferredWidthInChar];
            Arrays.fill(a, ' ');
            this.stringStuff = String.valueOf(a);
        } else
            this.stringStuff = "123456789012345678901234567890";

        this.combo = new ISearchableCombo<IComboSelectionItem>(ComboLockedMode.LOCKED, 1, this.stringStuff.length());
        // it's this.req which handles the selection so the graphical combo should never pick a new
        // selection by itself
        this.combo.setOnRemovingOrReplacingSelection(NewSelection.NO);
        // ComboSQLRequest return items with children at the start (e.g. Room <| Building <| Site)
        this.combo.setForceDisplayStart(true);
        this.combo.setIncludeEmpty(addUndefined);
        this.combo.getActions().add(new AbstractAction("Recharger") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // ignore cache since a user explicitly asked for an update
                fillCombo(null, false);
            }
        });

        this.emptySupp = new EmptyChangeSupport(this);
        this.supp = new ValueChangeSupport<Integer>(this);
    }

    /**
     * Specify which item will be selected the first time the combo is filled (unless setValue() is
     * called before the fill).
     * 
     * @param firstFillTransf will be passed the items and should return the wanted selection.
     */
    public final void setFirstFillSelection(ITransformer<List<IComboSelectionItem>, IComboSelectionItem> firstFillTransf) {
        this.req.setFirstFillSelection(firstFillTransf);
    }

    @Override
    public void added(RIVPanel sqlComp, SQLRowItemView v) {
        final SQLTable foreignTable = v.getField().getForeignTable();
        if (!this.hasModel()) {
            this.uiInit(sqlComp.getDirectory().getElement(foreignTable).getComboRequest());
        } else {
            if (this.getRequest().getPrimaryTable() != foreignTable)
                throw new IllegalArgumentException("Tables are different " + getRequest().getPrimaryTable().getSQLName() + " != " + foreignTable.getSQLName());
        }
    }

    /**
     * Init de l'interface graphique.
     * 
     * @param req which table to display and how.
     */
    public final void uiInit(final ComboSQLRequest req) {
        this.uiInit(new IComboModel(req));
    }

    protected final boolean hasModel() {
        return this.req != null;
    }

    public final IComboModel getModel() {
        return this.req;
    }

    public final void uiInit(final IComboModel req) {
        if (hasModel())
            throw new IllegalStateException(this + " already inited.");

        this.req = req;
        // listeners
        this.addModelListener("updating", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateEnabled();
            }
        });
        // since modelValueChanged() updates the UI use selectedValue (i.e. IComboSelectionItem
        // with a label)
        this.addModelListener("selectedValue", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                modelValueChanged();
            }
        });
        this.addModelListener("wantedID", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                SQLRequestComboBox.this.supp.fireValueChange();
            }
        });
        this.req.addEmptyListener(new EmptyListener() {
            @Override
            public void emptyChange(EmptyObj src, boolean newValue) {
                SQLRequestComboBox.this.emptySupp.fireEmptyChange(newValue);
            }
        });

        // remove listeners to allow this to be gc'd
        this.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0)
                    updateListeners();
            }
        });

        // initial state ; since we're in the EDT, the DISPLAYABILITY cannot change between
        // addHierarchyListener() and here
        updateListeners();

        this.addAncestorListener(new AncestorListener() {

            @Override
            public void ancestorAdded(AncestorEvent event) {

                // Appel dans un invokeLater, sinon la frame peut passer en arriere plan (exemple :
                // CloturePayeMensuellePanel)
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateListeners();
                    }
                });
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                updateListeners();
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                // don't care
            }
        });
        FontUtils.setFontFor(this.combo, "ComboBox", this.getRequest().getSeparatorsChars());
        // try to speed up that damned JList, as those fine Swing engineers put it "This is
        // currently hacky..."
        for (int i = 0; i < this.combo.getUI().getAccessibleChildrenCount(this.combo); i++) {
            final Accessible acc = this.combo.getUI().getAccessibleChild(this.combo, i);
            if (acc instanceof ComboPopup) {
                final ComboPopup cp = (ComboPopup) acc;
                cp.getList().setPrototypeCellValue(new IComboSelectionItem(-1, this.stringStuff));
            }
        }

        this.combo.setIconFactory(new ITransformer<IComboSelectionItem, Icon>() {
            @Override
            public Icon transformChecked(final IComboSelectionItem input) {
                return getIconFor(input);
            }
        });
        this.combo.initCache(this.req);
        this.combo.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                comboValueChanged();
            }
        });
        // synchronize with the state of req (after adding our value listener)
        this.modelValueChanged();

        // getValidSate() depends on this.req
        this.supp.fireValidChange();
        // and on this.combo.getValidState()
        this.combo.addValidListener(new ValidListener() {
            @Override
            public void validChange(ValidObject src, ValidState newValue) {
                SQLRequestComboBox.this.supp.fireValidChange();
            }
        });

        this.combo.addPropertyChangeListener("textCompFocused", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateEnabled();
            }
        });

        this.uiLayout();

        // Initialise UI : mode attribute was set in the constructor, but the UI wasn't updated
        // since it wasn't created (and lacking the request). If updateEnabled() doesn't set the
        // mode, set it with the value set in the constructor.
        if (!updateEnabled())
            this.setInteractionMode(this.getInteractionMode());
    }

    // return true if setEnabled() was called
    private final boolean updateEnabled() {
        boolean res = false;
        if (isDisabledState()) {
            // don't overwrite, happens if updateEnabled() is called twice and isDisabledState() is
            // false both times. In that case getEnabled() would return DISABLED
            if (this.modeToSelect == null) {
                this.modeToSelect = this.getInteractionMode();
                // ne pas interagir pendant le chargement
                this.setEnabled(InteractionMode.DISABLED, true);
                res = true;
            }
        } else {
            // only use modeToSelect once. If updateEnabled() is called twice and isDisabledState()
            // is true both times and modeToSelect wasn't cleared it could be obsolete (another
            // setEnabled() could have been called)
            if (this.modeToSelect != null) {
                final InteractionMode m = this.modeToSelect;
                this.modeToSelect = null;
                this.setEnabled(m, true);
                res = true;
            }
        }
        return res;
    }

    // disable combo when updating, except if the user is currently using it (e.g. when using a
    // request that search the DB interactively)
    private final boolean isDisabledState() {
        return this.isUpdating() && !this.combo.getTextComp().isFocusOwner();
    }

    public final List<Action> getActions() {
        return this.combo.getActions();
    }

    protected void updateListeners() {
        if (hasModel()) {
            final SleepState newState;
            if (!this.isDisplayable())
                newState = SleepState.HIBERNATING;
            else if (!this.isShowing())
                newState = SleepState.SLEEPING;
            else
                newState = SleepState.AWAKE;
            this.req.setSleepState(newState);
        }
    }

    public final ComboSQLRequest getRequest() {
        return this.req.getRequest();
    }

    protected void uiLayout() {
        this.setLayout(new GridLayout(1, 1));
        this.add(this.combo);
    }

    public void setDebug(boolean trace) {
        this.req.setDebug(trace);
        this.combo.setDebug(trace);
    }

    /**
     * Whether this combo is allowed to delay {@link #fillCombo()} when it isn't visible.
     * 
     * @param sleepAllowed <code>true</code> if reloads can be delayed.
     */
    public final void setSleepAllowed(boolean sleepAllowed) {
        this.req.setSleepAllowed(sleepAllowed);
    }

    public final boolean isSleepAllowed() {
        return this.req.isSleepAllowed();
    }

    /**
     * Reload this combo. This method is thread-safe.
     */
    public synchronized final void fillCombo() {
        this.fillCombo(null);
    }

    public synchronized final void fillCombo(final Runnable r) {
        this.fillCombo(r, true);
    }

    public synchronized final void fillCombo(final Runnable r, final boolean readCache) {
        this.req.fillCombo(r, readCache);
    }

    // combo

    public final List<IComboSelectionItem> getItems() {
        return this.getComboModel().getList();
    }

    private DefaultIMutableListModel<IComboSelectionItem> getComboModel() {
        return (DefaultIMutableListModel<IComboSelectionItem>) this.combo.getCache();
    }

    public final IComboSelectionItem getItem(int id) {
        return this.req.getItem(id);
    }

    public final void addModelListener(String propName, PropertyChangeListener l) {
        this.req.addListener(propName, l);
    }

    public final void removeModelListener(String propName, PropertyChangeListener l) {
        this.req.rmListener(propName, l);
    }

    // *** value

    public final void resetValue() {
        this.setValue((Integer) null);
    }

    public final void setValue(int id) {
        this.req.setValue(id);
    }

    public final void setValue(Integer id) {
        if (id == null)
            this.setValue(SQLRow.NONEXISTANT_ID);
        else
            this.setValue((int) id);
    }

    public final void setValue(SQLRowAccessor r) {
        this.setValue(r == null ? null : r.getID());
    }

    public final Integer getValue() {
        final int id = this.getWantedID();
        return id == SQLRow.NONEXISTANT_ID ? null : id;
    }

    /**
     * Renvoie l'ID de l'item sélectionné.
     * 
     * @return l'ID de l'item sélectionné, <code>SQLRow.NONEXISTANT_ID</code> si combo vide.
     */
    public final int getSelectedId() {
        return this.req.getSelectedId();
    }

    public final int getWantedID() {
        return this.req.getWantedID();
    }

    /**
     * The selected row or <code>null</code> if this is empty.
     * 
     * @return a SQLRow (non fetched) or <code>null</code>.
     */
    public final SQLRow getSelectedRow() {
        if (this.isEmpty())
            return null;
        else {
            return this.req.getWantedRow();
        }
    }

    /**
     * The currently selected row in the UI. The result depends on the
     * {@link ComboSQLRequest#keepRows(KeepMode) keep mode} of the {@link #getRequest() request}.
     * 
     * @return the currently selected row, <code>null</code> if none or for {@link KeepMode#NONE}.
     */
    public final SQLRowAccessor getSelectedRowAccessor() {
        final IComboSelectionItem selectedValue = this.req.getSelectedValue();
        if (selectedValue == null)
            return null;

        final SQLRowAccessor res = selectedValue.getRow();
        if (res == null || res instanceof SQLRow)
            return res;
        else
            return ((SQLRowValues) res).toImmutable();
    }

    private void modelValueChanged() {
        final IComboSelectionItem newValue = this.req.getSelectedValue();
        // user makes invalid edit => combo invalid=true and value=null => model value=null
        // and if we call combo.setValue() it will change invalid to false
        if (this.combo.getValue() != newValue)
            this.combo.setValue(newValue);
    }

    private final void comboValueChanged() {
        // since we used NewSelection.NO for this.combo it never generates spurious events
        // i.e. this method is only called by user action
        this.req.setValue(this.combo.getValue());
    }

    /**
     * Whether missing item are fetched from the database. If {@link #setValue(Integer)} is called
     * with an ID not present in the list and addMissingItem is <code>true</code> then that ID will
     * be fetched and added to the list, if it is <code>false</code> the selection will be cleared.
     * 
     * @return <code>true</code> if missing item are fetched.
     */
    public final boolean addMissingItem() {
        return this.req.addMissingItem();
    }

    public final void setAddMissingItem(boolean addMissingItem) {
        this.req.setAddMissingItem(addMissingItem);
    }

    @Override
    public void setInteractionMode(InteractionMode mode) {
        this.setEnabled(mode);
    }

    public final void setEnabled(InteractionMode mode) {
        this.setEnabled(mode, false);
    }

    private final void setEnabled(InteractionMode mode, boolean priv) {
        assert SwingUtilities.isEventDispatchThread();
        if (!priv && this.isDisabledState()) {
            this.modeToSelect = mode;
        } else {

            this.mode = mode;
            modeChanged(mode);
        }
    }

    protected void modeChanged(InteractionMode mode) {
        mode.applyTo(this.combo);
    }

    @Override
    public final InteractionMode getInteractionMode() {
        return this.mode;
    }

    public String toString() {
        return this.getClass().getName() + " " + this.req;
    }

    @Override
    public final boolean isEmpty() {
        return this.req == null || this.req.isEmpty();
    }

    @Override
    public final void addEmptyListener(EmptyListener l) {
        this.emptySupp.addEmptyListener(l);
    }

    @Override
    public void removeEmptyListener(EmptyListener l) {
        this.emptySupp.removeEmptyListener(l);
    }

    public final void addValueListener(PropertyChangeListener l) {
        this.supp.addValueListener(l);
    }

    public final void rmValueListener(PropertyChangeListener l) {
        this.supp.rmValueListener(l);
    }

    public final void addItemsListener(PropertyChangeListener l) {
        this.addItemsListener(l, false);
    }

    /**
     * Adds a listener on the items of this combo.
     * 
     * @param l the listener.
     * @param all <code>true</code> if <code>l</code> should be called for all changes, including UI
     *        ones (e.g. adding a '-- loading --' item).
     */
    public final void addItemsListener(PropertyChangeListener l, final boolean all) {
        this.req.addItemsListener(l, all);
    }

    public final void rmItemsListener(PropertyChangeListener l) {
        this.req.rmItemsListener(l);
    }

    @Override
    public ValidState getValidState() {
        // we are valid if we can return a value and getValue() needs this.req
        return ValidState.getNoReasonInstance(hasModel()).and(this.combo.getValidState());
    }

    @Override
    public final void addValidListener(ValidListener l) {
        this.supp.addValidListener(l);
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.supp.removeValidListener(l);
    }

    private Icon getIconFor(IComboSelectionItem value) {
        final Icon i;
        if (value == null) {
            // happens when the combo is empty
            i = null;
        } else {
            final int flag = value.getFlag();
            if (flag == IComboSelectionItem.WARNING_FLAG)
                i = new ImageIcon(SQLRequestComboBox.class.getResource("warning.png"));
            else if (flag == IComboSelectionItem.ERROR_FLAG)
                i = new ImageIcon(SQLRequestComboBox.class.getResource("error.png"));
            else
                i = null;
        }
        return i;
    }

    public final JComponent getComp() {
        return this;
    }

    public JTextComponent getTextComp() {
        return this.combo.getTextComp();
    }

    @Override
    public Collection<JComponent> getPulseComponents() {
        return Arrays.<JComponent> asList(this.combo);
    }

    // *** search

    public final void search(SearchSpec spec) {
        this.req.search(spec);
    }

    public final boolean isUpdating() {
        return this.req.isUpdating();
    }

    // completion
    public final void setCompletionMode(SearchMode m) {
        this.combo.setCompletionMode(m);
    }

}
