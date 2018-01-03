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
 
 package org.openconcerto.erp.core.humanresources.payroll.action;

import org.openconcerto.erp.core.common.ui.ListeViewPanel;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.view.list.IListe;

public class ListeFichePayeAddPanel extends ListeViewPanel {
    {
        this.buttonModifier.setText("Voir");
    }

    public ListeFichePayeAddPanel(SQLElement component) {
        super(component);
    }

    public ListeFichePayeAddPanel(SQLElement component, IListe list) {
        super(component, list);
        this.buttonEffacer.setVisible(true);
    }

}