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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.SQLSyntax;

/**
 * Construct an CREATE TABLE statement that can be relocated (its foreign keys do not point to a
 * specific table).
 * 
 * @author Sylvain
 */
public final class SQLCreateMoveableTable extends SQLCreateTableBase<SQLCreateMoveableTable> {

    public SQLCreateMoveableTable(SQLSyntax syntax, String name) {
        this(syntax, null, name);
    }

    public SQLCreateMoveableTable(final SQLSyntax syntax, final String root, final String name) {
        super(syntax, root, name);
    }
}
