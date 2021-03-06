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
 
 package org.openconcerto.sql.users;

import org.openconcerto.sql.element.ConfSQLElement;

import java.util.ArrayList;
import java.util.List;

public class CompanyAccessSQLElement extends ConfSQLElement {

    public CompanyAccessSQLElement() {
        super("ACCES_SOCIETE");

    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_USER_COMMON");
        l.add("ID_SOCIETE_COMMON");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_USER_COMMON");
        l.add("ID_SOCIETE_COMMON");
        return l;
    }

}
