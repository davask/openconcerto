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

import java.util.HashMap;
import java.util.Map;

import org.openconcerto.erp.core.common.ui.AbstractVenteArticleItemTable;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;

public class FactureSituationItemTable extends AbstractVenteArticleItemTable {

    public FactureSituationItemTable() {
        super();
        this.control.setVisible(false);
    }

    @Override
    protected String getConfigurationFileName() {
        return "Table_Facture_Situation.xml";
    }

    @Override
    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");
    }

    protected Map<String, Boolean> getCustomVisibilityMap() {
        Map<String, Boolean> map = new HashMap<String, Boolean>();
        map.put("CODE", Boolean.FALSE);
        map.put("POURCENT_FACTURABLE", Boolean.TRUE);
        map.put("CODE_DOUANIER", Boolean.FALSE);
        map.put("ID_PAYS", Boolean.FALSE);
        map.put("T_DEVISE", Boolean.FALSE);
        map.put("P_HA", Boolean.FALSE);

        return map;
    }
}
