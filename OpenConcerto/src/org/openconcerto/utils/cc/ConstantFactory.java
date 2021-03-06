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
 
 package org.openconcerto.utils.cc;

public class ConstantFactory<E> implements IFactory<E> {

    static private final ConstantFactory<Object> nullFactory = new ConstantFactory<Object>(null);

    @SuppressWarnings("unchecked")
    static public final <F> ConstantFactory<F> nullFactory() {
        return (ConstantFactory<F>) nullFactory;
    }

    static public final <E, F> ITransformer<E, F> createTransformer(final F obj) {
        return new ITransformer<E, F>() {
            @Override
            public F transformChecked(E input) {
                return obj;
            }
        };
    }

    private final E obj;

    public ConstantFactory(E obj) {
        super();
        this.obj = obj;
    }

    @Override
    public E createChecked() {
        return this.obj;
    }
}
