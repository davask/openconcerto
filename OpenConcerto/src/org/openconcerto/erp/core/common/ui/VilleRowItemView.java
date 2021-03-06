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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.map.model.Ville;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.itemview.VWRowItemView;
import org.openconcerto.ui.component.InteractionMode;
import org.openconcerto.ui.valuewrapper.ValueWrapper;

public class VilleRowItemView extends VWRowItemView<Ville> {

    public VilleRowItemView(ValueWrapper<Ville> wrapper) {
        super(wrapper);
    }

    @Override
    public void setEditable(InteractionMode b) {
        if (this.getComp() != null) {
            this.getComp().setEnabled(b.isEnabled());
        }
    }

    @Override
    public void show(SQLRowAccessor r) {
        final String fieldName0 = getFields().get(0).getName();
        final String fieldName1 = getFields().get(1).getName();
        if (r.getFields().contains(fieldName0) && r.getFields().contains(fieldName1)) {
            String cp = r.getString(fieldName0);
            String name = r.getString(fieldName1);
            final Ville villeFromVilleEtCode = Ville.getVilleFromVilleEtCode(name + " (" + cp + ")");
            // get a matching Ville
            if (villeFromVilleEtCode != null) {
                this.getWrapper().setValue(villeFromVilleEtCode);
            } else {
                this.getWrapper().setValue(new Ville(name, 0, 0, 0, cp));
            }
        }
    }

    @Override
    public void update(SQLRowValues vals) {
        vals.put(getFields().get(1).getName(), this.isEmpty() ? SQLRowValues.SQL_DEFAULT : this.getWrapper().getValue().getName());
        vals.put(getFields().get(0).getName(), this.isEmpty() ? SQLRowValues.SQL_DEFAULT : this.getWrapper().getValue().getCodepostal());
    }

}
