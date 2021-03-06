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
 
 package org.openconcerto.ui.filters;

import java.text.DecimalFormat;

public class FloatFormatFilter extends NumberFormatFilter<Number> {

    static public final DecimalFormat floatFormat = (DecimalFormat) DecimalFormatFilter.floatFormat.clone();
    static {
        floatFormat.setParseBigDecimal(false);
    }

    public FloatFormatFilter() {
        super(floatFormat, Number.class);
    }

    /**
     * Format.format() prints the exact value (eg 1.09999985) toString() prints a more human
     * representation (eg 1.1) see {@link Float#toString(float)}.
     */
    public String format(Number o) {
        return o.toString();
    }

}
