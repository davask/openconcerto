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
 
 package org.openconcerto.erp.injector;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLInjector;

public class CommandeBlEltSQLInjector extends SQLInjector {
    public CommandeBlEltSQLInjector(final DBRoot root) {
        super(root, "COMMANDE_CLIENT_ELEMENT", "BON_DE_LIVRAISON_ELEMENT", false);
        createDefaultMap();
        mapDefaultValues(getDestination().getField("QTE_LIVREE"), Integer.valueOf(0));
        if (getDestination().contains("ID_COMMANDE_CLIENT_ELEMENT")) {
            map(getSource().getKey(), getDestination().getField("ID_COMMANDE_CLIENT_ELEMENT"));
        }
    }
}
