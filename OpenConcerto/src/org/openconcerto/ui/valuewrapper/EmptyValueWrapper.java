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
 
 package org.openconcerto.ui.valuewrapper;

import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.checks.EmptyObjFromVO;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;

import java.beans.PropertyChangeListener;

import javax.swing.JComponent;

/**
 * A ValueWrapper that knows if it is empty or not.
 * 
 * @author Sylvain
 * 
 * @param <T> the type of value the component has.
 */
public class EmptyValueWrapper<T> implements ValueWrapper<T>, EmptyObj {

    private final ValueWrapper<T> vw;
    private final EmptyObj eo;

    public EmptyValueWrapper(final ValueWrapper<T> vw) {
        this(vw, null);
    }

    public EmptyValueWrapper(final ValueWrapper<T> vw, IPredicate<T> emptyPred) {
        super();
        this.vw = vw;
        this.eo = new EmptyObjFromVO<T>(this, emptyPred == null ? EmptyObjFromVO.<T> getDefaultPredicate() : emptyPred);
    }

    public void addValidListener(final ValidListener l) {
        this.vw.addValidListener(l);
    }

    public void removeValidListener(final ValidListener l) {
        this.vw.removeValidListener(l);
    }

    public void addValueListener(final PropertyChangeListener l) {
        this.vw.addValueListener(l);
    }

    public JComponent getComp() {
        return this.vw.getComp();
    }

    public T getValue() {
        return this.vw.getValue();
    }

    @Override
    public ValidState getValidState() {
        return this.vw.getValidState();
    }

    public void rmValueListener(final PropertyChangeListener l) {
        this.vw.rmValueListener(l);
    }

    public void setValue(final T val) {
        this.vw.setValue(val);
    }

    public void resetValue() {
        this.vw.resetValue();
    }

    @Override
    public void addEmptyListener(EmptyListener l) {
        this.eo.addEmptyListener(l);
    }

    @Override
    public void removeEmptyListener(EmptyListener l) {
        this.eo.removeEmptyListener(l);
    }

    @Override
    public boolean isEmpty() {
        return this.eo.isEmpty();
    }

}
