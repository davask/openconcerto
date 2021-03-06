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
 
 package org.openconcerto.erp.core.sales.pos.ui;

import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JRadioButton;

public class POSRadioButton extends JRadioButton {
    public POSRadioButton(String text, Icon icon, boolean selected) {
        super(text, icon, selected);
        customize();
    }

    public POSRadioButton(String text) {
        super(text);
        customize();
    }

    public void customize() {
        final Font f = getFont().deriveFont(20f);
        this.setFont(f);
        this.setOpaque(false);
    }
}
