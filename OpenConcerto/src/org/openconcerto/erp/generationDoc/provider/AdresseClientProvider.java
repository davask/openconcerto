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
 
 package org.openconcerto.erp.generationDoc.provider;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueContext;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProvider;

public abstract class AdresseClientProvider implements SpreadSheetCellValueProvider {

    public static int ADRESSE_PRINCIPALE = 0;
    public static int ADRESSE_FACTURATION = 1;
    public static int ADRESSE_LIVRAISON = 2;

    @Override
    public abstract Object getValue(SpreadSheetCellValueContext context);

    public SQLRowAccessor getAdresse(SQLRowAccessor r, int type) {

        // Adresse spécifique
        if (type != ADRESSE_PRINCIPALE) {
            String field;
            if (type == ADRESSE_FACTURATION) {
                field = "ID_ADRESSE";
            } else {
                field = "ID_ADRESSE_LIVRAISON";
            }
            if (r.getTable().contains(field)) {
                SQLRowAccessor rAdr = r.getForeign(field);
                if (rAdr != null && !rAdr.isUndefined()) {
                    return rAdr;
                }
            }
        }


        SQLRowAccessor rCli = r.getForeign("ID_CLIENT");
        SQLRowAccessor adrResult;
            adrResult = rCli.getForeign("ID_ADRESSE");
        if (type != ADRESSE_PRINCIPALE) {
            String field;
            if (type == ADRESSE_FACTURATION) {
                field = "ID_ADRESSE_F";
            } else {
                field = "ID_ADRESSE_L";
            }
            SQLRowAccessor rAdr = rCli.getForeign(field);
            if (rAdr != null && !rAdr.isUndefined()) {
                adrResult = rAdr;
            }
        }

        if (type == ADRESSE_FACTURATION && r.getTable().contains("ID_CLIENT_DEPARTEMENT")) {
            if (!r.isForeignEmpty("ID_CLIENT_DEPARTEMENT")) {
                SQLRowAccessor rDpt = r.getForeign("ID_CLIENT_DEPARTEMENT");
                if (!rDpt.isForeignEmpty("ID_ADRESSE")) {
                    adrResult = rDpt.getForeign("ID_ADRESSE");
                }
            }
        }

        return adrResult;
    }

}
