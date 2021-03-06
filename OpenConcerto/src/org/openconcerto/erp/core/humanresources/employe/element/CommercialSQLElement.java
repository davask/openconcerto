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
 
 package org.openconcerto.erp.core.humanresources.employe.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.utils.ListMap;

import java.util.ArrayList;
import java.util.List;

public class CommercialSQLElement extends ComptaSQLConfElement {

    public CommercialSQLElement() {
        super("COMMERCIAL", "un commercial", "commerciaux");
    }

    public CommercialSQLElement(String tableName, String singular, String plural) {
        super(tableName, singular, plural);
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("PRENOM");
        l.add("FONCTION");
        l.add("TEL_STANDARD");
        l.add("TEL_DIRECT");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("PRENOM");
        l.add("NOM");
        l.add("FONCTION");
        return l;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        final ListMap<String, String> res = new ListMap<String, String>();
        res.putCollection(null, "NOM");
        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new CommercialSQLComponent(this);
    }

    @Override
    protected String createCode() {
        return super.createCodeFromPackage() + ".salesman";
    }
}
