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
 
 package org.openconcerto.ui.group.modifier;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.GroupModifier;
import org.openconcerto.ui.group.Item;

public class MoveAfterItemModifier extends HierarchyModifier {

    private String afterId;

    public MoveAfterItemModifier(String itemId, String afterId) {
        super(itemId);
        if (afterId == null) {
            throw new IllegalArgumentException("null afterId");
        }
        this.afterId = afterId;
    }

    @Override
    public void applyOn(Group g) {
        // Remove Item from parent
        Item i = g.getDescFromID(getItemId());
        i.getParent().remove(getItemId());
        // New parent
        Group p = g.getDescFromID(afterId).getParent();
        int index1 = p.getIndex(afterId);
        Integer order1 = p.getOrder(index1);
        Integer order2 = order1 + 200;
        if (index1 < p.getSize() - 1) {
            order2 = p.getOrder(index1 + 1);
        }
        p.add(i, (order1 + order2) / 2);
    }

    @Override
    public boolean canBeAppliedOn(Group g) {
        return g.contains(afterId) && g.contains(getItemId());
    }

    @Override
    public boolean isCompatibleWith(GroupModifier g) {
        if (g instanceof HierarchyModifier) {
            final HierarchyModifier gr = (HierarchyModifier) g;
            if (gr.getItemId().equals(getItemId())) {
                return false;
            }
        }
        return true;
    }

}
