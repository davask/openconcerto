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
 
 package org.openconcerto.sql.model;

import static org.openconcerto.xml.JDOM2Utils.OUTPUTTER;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.SQLSelect.ArchiveMode;
import org.openconcerto.sql.model.SQLSelect.LockStrength;
import org.openconcerto.sql.model.SQLSyntax.ConstraintType;
import org.openconcerto.sql.model.SQLTableEvent.Mode;
import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Link.Rule;
import org.openconcerto.sql.model.graph.SQLKey;
import org.openconcerto.sql.model.graph.SQLKey.Type;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.SQLCreateMoveableTable;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.SetMap;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.Tuple3;
import org.openconcerto.utils.Value;
import org.openconcerto.utils.cc.CopyOnWriteMap;
import org.openconcerto.utils.cc.CustomEquals;
import org.openconcerto.utils.change.CollectionChangeEventCreator;

import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.ResultSetHandler;
import org.jdom2.Element;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;

/**
 * Une table SQL. Connait ses champs, notamment sa clef primaire et ses clefs externes. Une table
 * peut aussi faire des diagnostic sur son intégrité, ou sur la validité d'une valeur d'un de ses
 * champs. Enfin elle permet d'accéder aux lignes qui la composent.
 * 
 * @author ILM Informatique 4 mai 2004
 * @see #getField(String)
 * @see #getKey()
 * @see #getForeignKeys()
 * @see #checkIntegrity()
 * @see #checkValidity(String, int)
 * @see #getRow(int)
 */
public final class SQLTable extends SQLIdentifier implements SQLData, TableRef {

    private static final String UNDEF_TABLE_TABLENAME_FIELD = "TABLENAME";
    private static final String UNDEF_TABLE_ID_FIELD = "UNDEFINED_ID";

    /**
     * The {@link DBRoot#setMetadata(String, String) meta data} configuring the policy regarding
     * undefined IDs for a particular root. Can be either :
     * <dl>
     * <dt>inDB</dt>
     * <dd>all undefined IDs must be in {@value #undefTable}. Allow different IDs like "min" but
     * without the performance penalty</dd>
     * <dt>min</dt>
     * <dd>for min("ID")</dd>
     * <dt>nonexistant</dt>
     * <dd>(the default) {@link SQLRow#NONEXISTANT_ID}</dd>
     * <dt><i>any other value</i></dt>
     * <dd>parsed as a number</dd>
     * </dl>
     */
    public static final String UNDEFINED_ID_POLICY = "undefined ID policy";
    public static final String undefTable = SQLSchema.FWK_TABLENAME_PREFIX + "UNDEFINED_IDS";
    // {SQLSchema=>{TableName=>UndefID}}
    private static final Map<SQLSchema, Map<String, Number>> UNDEFINED_IDs = new HashMap<SQLSchema, Map<String, Number>>();
    private static final ResultSetHandler UNDEF_RSH = new ResultSetHandler() {
        @Override
        public Object handle(ResultSet rs) throws SQLException {
            final Map<String, Number> res = new HashMap<String, Number>();
            while (rs.next()) {
                res.put(rs.getString(UNDEF_TABLE_TABLENAME_FIELD), (Number) rs.getObject(UNDEF_TABLE_ID_FIELD));
            }
            return res;
        }
    };

    @SuppressWarnings("unchecked")
    private static final Map<String, Number> getUndefIDs(final SQLSchema schema) {
        if (!UNDEFINED_IDs.containsKey(schema)) {
            final Map<String, Number> r;
            if (schema.contains(undefTable)) {
                final SQLBase b = schema.getBase();
                final SQLTable undefT = schema.getTable(undefTable);
                final SQLSelect sel = new SQLSelect().addSelectStar(undefT);
                // don't use the cache as the result is stored in UNDEFINED_IDs
                r = (Map<String, Number>) b.getDataSource().execute(sel.asString(), new IResultSetHandler(UNDEF_RSH, false));
                // be called early, since it's more likely that some transaction will create table,
                // set its undefined ID, then use it in requests, than some other transaction
                // needing the undefined ID. TODO The real fix is one tree per transaction.
                undefT.addTableModifiedListener(new ListenerAndConfig(new SQLTableModifiedListener() {
                    @Override
                    public void tableModified(SQLTableEvent evt) {
                        synchronized (UNDEFINED_IDs) {
                            UNDEFINED_IDs.remove(schema);
                            undefT.removeTableModifiedListener(this);
                        }
                    }
                }, false));
            } else {
                r = Collections.emptyMap();
            }
            UNDEFINED_IDs.put(schema, r);
        }
        return UNDEFINED_IDs.get(schema);
    }

    static final void removeUndefID(SQLSchema s) {
        synchronized (UNDEFINED_IDs) {
            UNDEFINED_IDs.remove(s);
        }
    }

    static final Tuple2<Boolean, Number> getUndefID(SQLSchema b, String tableName) {
        synchronized (UNDEFINED_IDs) {
            final Map<String, Number> map = getUndefIDs(b);
            return Tuple2.create(map.containsKey(tableName), map.get(tableName));
        }
    }

    private static final SQLCreateMoveableTable getCreateUndefTable(SQLSyntax syntax) {
        final SQLCreateMoveableTable createTable = new SQLCreateMoveableTable(syntax, undefTable);
        createTable.addVarCharColumn(UNDEF_TABLE_TABLENAME_FIELD, 250);
        createTable.addColumn(UNDEF_TABLE_ID_FIELD, syntax.getIDType());
        createTable.setPrimaryKey(UNDEF_TABLE_TABLENAME_FIELD);
        return createTable;
    }

    private static final SQLTable getUndefTable(SQLSchema schema, boolean create) throws SQLException {
        final SQLTable undefT = schema.getTable(undefTable);
        if (undefT != null || !create) {
            return undefT;
        } else {
            schema.getDBSystemRoot().getDataSource().execute(getCreateUndefTable(SQLSyntax.get(schema)).asString(schema.getDBRoot().getName()));
            schema.updateVersion();
            return schema.fetchTable(undefTable);
        }
    }

    public static final void setUndefID(SQLSchema schema, String tableName, Integer value) throws SQLException {
        setUndefIDs(schema, Collections.singletonMap(tableName, value));
    }

    // return modified count
    public static final int setUndefIDs(SQLSchema schema, Map<String, ? extends Number> values) throws SQLException {
        synchronized (UNDEFINED_IDs) {
            final SQLTable undefT = getUndefTable(schema, true);
            final SQLType undefType = undefT.getField(UNDEF_TABLE_ID_FIELD).getType();
            final List<List<String>> toInsert = new ArrayList<List<String>>();
            final List<List<String>> toUpdate = new ArrayList<List<String>>();
            final Map<String, Number> currentValues = getUndefIDs(schema);
            final SQLBase b = schema.getBase();
            final SQLSystem system = b.getServer().getSQLSystem();
            for (final Entry<String, ? extends Number> e : values.entrySet()) {
                final String tableName = e.getKey();
                final Number undefValue = e.getValue();
                final List<List<String>> l;
                if (!currentValues.containsKey(tableName)) {
                    l = toInsert;
                } else if (CompareUtils.equals(currentValues.get(tableName), undefValue)) {
                    l = null;
                } else {
                    l = toUpdate;
                }
                if (l != null) {
                    final String undefSQL;
                    if (undefValue == null && system == SQLSystem.POSTGRESQL)
                        // column "UNDEFINED_ID" is of type integer but expression is of type text
                        undefSQL = "cast( NULL as " + undefType.getTypeName() + ")";
                    else
                        undefSQL = undefType.toString(undefValue);
                    l.add(Arrays.asList(b.quoteString(tableName), undefSQL));
                }
            }
            final SQLSyntax syntax = schema.getDBSystemRoot().getSyntax();
            if (toInsert.size() > 0) {
                // INSERT
                SQLRowValues.insertCount(undefT, "(" + SQLSyntax.quoteIdentifiers(Arrays.asList(UNDEF_TABLE_TABLENAME_FIELD, UNDEF_TABLE_ID_FIELD)) + ") " + syntax.getValues(toInsert, 2));
            }
            if (toUpdate.size() > 0) {
                // UPDATE
                // h2 doesn't support multi-table UPDATE
                if (system == SQLSystem.H2) {
                    final StringBuilder updates = new StringBuilder();
                    for (final List<String> l : toUpdate) {
                        final UpdateBuilder update = new UpdateBuilder(undefT).set(UNDEF_TABLE_ID_FIELD, l.get(1));
                        update.setWhere(Where.createRaw(undefT.getField(UNDEF_TABLE_TABLENAME_FIELD).getFieldRef() + " = " + l.get(0)));
                        updates.append(update.asString());
                        updates.append(";\n");
                    }
                    schema.getDBSystemRoot().getDataSource().execute(updates.toString());
                } else {
                    final UpdateBuilder update = new UpdateBuilder(undefT);
                    final String constantTableAlias = "newUndef";
                    update.addRawTable(syntax.getConstantTable(toUpdate, constantTableAlias, Arrays.asList("t", "v")), null);
                    update.setWhere(Where.createRaw(undefT.getField(UNDEF_TABLE_TABLENAME_FIELD).getFieldRef() + " = " + new SQLName(constantTableAlias, "t").quote()));
                    update.set(UNDEF_TABLE_ID_FIELD, new SQLName(constantTableAlias, "v").quote());
                    schema.getDBSystemRoot().getDataSource().execute(update.asString());
                }
            }
            final int res = toInsert.size() + toUpdate.size();
            if (res > 0) {
                undefT.fireTableModified(SQLRow.NONEXISTANT_ID);
            }
            return res;
        }
    }

    static private boolean AFTER_TX_DEFAULT = true;

    static public void setDefaultAfterTransaction(final boolean val) {
        AFTER_TX_DEFAULT = val;
    }

    static public final class ListenerAndConfig {

        private final SQLTableModifiedListener l;
        private final Boolean afterTx;

        /**
         * Create a new instance.
         * 
         * @param l the listener.
         * @param afterTx <code>true</code> if <code>l</code> should only be called once a
         *        transaction is committed, <code>false</code> to be called in real-time (i.e.
         *        called a second time if a transaction is aborted), <code>null</code> to be called
         *        both in real-time and after the transaction succeeds.
         */
        public ListenerAndConfig(SQLTableModifiedListener l, Boolean afterTx) {
            super();
            if (l == null)
                throw new NullPointerException("Null listener");
            this.l = l;
            this.afterTx = afterTx;
        }

        public final SQLTableModifiedListener getListener() {
            return this.l;
        }

        public final Boolean callOnlyAfterTx() {
            return this.afterTx;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (this.afterTx == null ? 0 : this.afterTx.hashCode());
            result = prime * result + this.l.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final ListenerAndConfig other = (ListenerAndConfig) obj;
            return CompareUtils.equals(this.afterTx, other.afterTx) && this.l.equals(other.l);
        }
    }

    @GuardedBy("this")
    private String version;
    private final CopyOnWriteMap<String, SQLField> fields;
    @GuardedBy("this")
    private Set<SQLField> primaryKeys;
    // the vast majority of our code use getKey(), so cache it for performance
    @GuardedBy("this")
    private SQLField primaryKey;
    // true if there's at most 1 primary key
    @GuardedBy("this")
    private boolean primaryKeyOK;
    @GuardedBy("this")
    private Set<SQLField> keys;
    @GuardedBy("this")
    private Map<String, FieldGroup> fieldsGroups;
    @GuardedBy("this")
    private final Map<String, Trigger> triggers;
    // null means it couldn't be retrieved
    @GuardedBy("this")
    private Set<Constraint> constraints;
    // always immutable so that fire can iterate safely ; to modify it, simply copy it before
    // (adding listeners is a lot less common than firing events)
    @GuardedBy("listenersMutex")
    private List<ListenerAndConfig> tableModifiedListeners;
    @GuardedBy("listenersMutex")
    private final ListMap<TransactionPoint, FireState> transactions;
    private final TransactionListener txListener;
    private final Object listenersMutex = new String("tableModifiedListeners mutex");
    // the id that foreign keys pointing to this, can use instead of NULL
    // a null value meaning not yet known
    @GuardedBy("this")
    private Integer undefinedID;

    @GuardedBy("this")
    private String comment;
    @GuardedBy("this")
    private String type;

    // empty table
    SQLTable(SQLSchema schema, String name) {
        super(schema, name);
        this.tableModifiedListeners = Collections.emptyList();
        this.transactions = new ListMap<TransactionPoint, FireState>();
        this.txListener = new TransactionListener() {
            @Override
            public void transactionEnded(TransactionPoint point) {
                fireFromTransaction(point);
            }
        };
        // needed for getOrderedFields()
        this.fields = new CopyOnWriteMap<String, SQLField>() {
            @Override
            public Map<String, SQLField> copy(Map<? extends String, ? extends SQLField> src) {
                return new LinkedHashMap<String, SQLField>(src);
            }
        };
        assert isOrdered(this.fields);
        this.primaryKeys = Collections.emptySet();
        this.primaryKey = null;
        this.primaryKeyOK = true;
        this.keys = null;
        this.fieldsGroups = null;
        this.triggers = new HashMap<String, Trigger>();
        // by default non-null, ie ok, only set to null on error
        this.constraints = new HashSet<Constraint>();
        // not known
        this.undefinedID = null;
        assert !this.undefinedIDKnown();
    }

    // *** setter

    synchronized void clearNonPersistent() {
        this.triggers.clear();
        // non-null, see ctor
        this.constraints = new HashSet<Constraint>();
    }

    // * from XML

    void loadFields(Element xml) {
        synchronized (this) {
            this.version = SQLSchema.getVersion(xml);
        }

        final LinkedHashMap<String, SQLField> newFields = new LinkedHashMap<String, SQLField>();
        for (final Element elementField : xml.getChildren("field")) {
            final SQLField f = SQLField.create(this, elementField);
            newFields.put(f.getName(), f);
        }

        final Element primary = xml.getChild("primary");
        final List<String> newPrimaryKeys = new ArrayList<String>();
        for (final Element elementField : primary.getChildren("field")) {
            final String fieldName = elementField.getAttributeValue("name");
            newPrimaryKeys.add(fieldName);
        }

        synchronized (getTreeMutex()) {
            synchronized (this) {
                this.setState(newFields, newPrimaryKeys, null);

                final Element triggersElem = xml.getChild("triggers");
                if (triggersElem != null)
                    for (final Element triggerElem : triggersElem.getChildren()) {
                        this.addTrigger(Trigger.fromXML(this, triggerElem));
                    }

                final Element constraintsElem = xml.getChild("constraints");
                if (constraintsElem == null)
                    this.addConstraint((Constraint) null);
                else
                    for (final Element elem : constraintsElem.getChildren()) {
                        this.addConstraint(Constraint.fromXML(this, elem));
                    }

                final Element commentElem = xml.getChild("comment");
                if (commentElem != null)
                    this.setComment(commentElem.getText());
                this.setType(xml.getAttributeValue("type"));
            }
        }
    }

    synchronized private void addTrigger(final Trigger t) {
        this.triggers.put(t.getName(), t);
    }

    synchronized private void addConstraint(final Constraint c) {
        if (c == null) {
            this.constraints = null;
        } else {
            if (this.constraints == null)
                this.constraints = new HashSet<Constraint>();
            this.constraints.add(c);
        }
    }

    // * from JDBC

    public void fetchFields() throws SQLException {
        this.getBase().fetchTables(TablesMap.createBySchemaFromTable(this));
    }

    /**
     * Fetch fields from the passed args.
     * 
     * @param metaData the metadata.
     * @param rs the resultSet of a getColumns(), the cursor must be on a row.
     * @param version the version of the schema.
     * @return whether the <code>rs</code> has more row.
     * @throws SQLException if an error occurs.
     * @throws IllegalStateException if the current row of <code>rs</code> doesn't describe this.
     */
    boolean fetchFields(DatabaseMetaData metaData, ResultSet rs, String version) throws SQLException {
        if (!this.isUs(rs))
            throw new IllegalStateException("rs current row does not describe " + this);

        synchronized (getTreeMutex()) {
            synchronized (this) {
                this.version = version;

                // we need to match the database ordering of fields
                final LinkedHashMap<String, SQLField> newFields = new LinkedHashMap<String, SQLField>();
                // fields
                boolean hasNext = true;
                while (hasNext && this.isUs(rs)) {
                    final SQLField f = SQLField.create(this, rs);
                    newFields.put(f.getName(), f);
                    hasNext = rs.next();
                }

                final List<String> newPrimaryKeys = new ArrayList<String>();
                final ResultSet pkRS = metaData.getPrimaryKeys(this.getBase().getMDName(), this.getSchema().getName(), this.getName());
                while (pkRS.next()) {
                    newPrimaryKeys.add(pkRS.getString("COLUMN_NAME"));
                }

                this.setState(newFields, newPrimaryKeys, null);

                return hasNext;
            }
        }
    }

    void emptyFields() {
        this.setState(new LinkedHashMap<String, SQLField>(), Collections.<String> emptyList(), null);
    }

    private boolean isUs(final ResultSet rs) throws SQLException {
        final String n = rs.getString("TABLE_NAME");
        final String s = rs.getString("TABLE_SCHEM");
        return n.equals(this.getName()) && CompareUtils.equals(s, this.getSchema().getName());
    }

    void addTrigger(Map<String, Object> m) {
        this.addTrigger(new Trigger(this, m));
    }

    void addConstraint(Map<String, Object> m) {
        this.addConstraint(m == null ? null : new Constraint(this, m));
    }

    // must be called in setState() after fields have been set (for isRowable())
    private int fetchUndefID() {
        int res;
        final SQLField pk;
        synchronized (this) {
            pk = isRowable() ? this.getKey() : null;
        }
        if (pk != null) {
            final Tuple2<Boolean, Number> currentValue = getUndefID(this.getSchema(), this.getName());
            if (!currentValue.get0()) {
                try {
                    // no row
                    res = this.findMinID(pk);
                } catch (Exception e) {
                    // we ***** don't care
                    e.printStackTrace();
                    res = SQLRow.NONEXISTANT_ID;
                }
            } else {
                // a row
                final Number id = currentValue.get1();
                res = id == null ? SQLRow.NONEXISTANT_ID : id.intValue();
            }
        } else
            res = SQLRow.NONEXISTANT_ID;
        return res;
    }

    // no undef id found
    private int findMinID(SQLField pk) {
        final String debugUndef = "fwk_sql.debug.undefined_id";
        if (System.getProperty(debugUndef) != null)
            Log.get().warning("The system property '" + debugUndef + "' is deprecated, use the '" + UNDEFINED_ID_POLICY + "' metadata");

        final String policy = getSchema().getFwkMetadata(UNDEFINED_ID_POLICY);
        if (Boolean.getBoolean(debugUndef) || "min".equals(policy)) {
            final SQLSelect sel = new SQLSelect(true).addSelect(pk, "min");
            final Number undef = (Number) this.getBase().getDataSource().executeScalar(sel.asString());
            if (undef == null) {
                // empty table
                throw new IllegalStateException(this + " is empty, can not infer UNDEFINED_ID");
            } else {
                final SQLSyntax syntax = SQLSyntax.get(this);
                final String update = syntax.getInsertOne(new SQLName(this.getDBRoot().getName(), undefTable), Arrays.asList(UNDEF_TABLE_TABLENAME_FIELD, UNDEF_TABLE_ID_FIELD),
                        syntax.quoteString(this.getName()), String.valueOf(undef));
                Log.get().config("the first row (which should be the undefined):\n" + update);
                return undef.intValue();
            }
        } else if ("inDB".equals(policy)) {
            throw new IllegalStateException("Not in " + new SQLName(this.getDBRoot().getName(), undefTable) + " : " + this.getName());
        } else if (policy != null && !"nonexistant".equals(policy)) {
            final int res = Integer.parseInt(policy);
            if (res < SQLRow.MIN_VALID_ID)
                throw new IllegalStateException("ID is not valid : " + res);
            return res;
        } else {
            // by default assume NULL is used
            return SQLRow.NONEXISTANT_ID;
        }
    }

    // * from Java

    void mutateTo(SQLTable table) {
        synchronized (getTreeMutex()) {
            synchronized (this) {
                this.clearNonPersistent();
                this.version = table.version;
                this.setState(table.fields, table.getPKsNames(), table.undefinedID);
                for (final Trigger t : table.triggers.values()) {
                    this.addTrigger(new Trigger(this, t));
                }
                if (table.constraints == null) {
                    this.constraints = null;
                } else {
                    for (final Constraint c : table.constraints) {
                        this.constraints.add(new Constraint(this, c));
                    }
                }
                this.setType(table.getType());
                this.setComment(table.getComment());
            }
        }
    }

    // * update attributes

    static private <K, V> boolean isOrdered(Map<K, V> m) {
        if (m instanceof CopyOnWriteMap)
            return isOrdered(((CopyOnWriteMap<K, V>) m).copy(Collections.<K, V> emptyMap()));
        return (m instanceof LinkedHashMap);
    }

    private void setState(Map<String, SQLField> fields, final List<String> primaryKeys, final Integer undef) {
        assert isOrdered(fields);
        // checks new fields' table (don't use ==, see below)
        for (final SQLField newField : fields.values()) {
            if (!newField.getTable().getSQLName().equals(this.getSQLName()))
                throw new IllegalArgumentException(newField + " is in table " + newField.getTable().getSQLName() + " not us: " + this.getSQLName());
        }
        synchronized (getTreeMutex()) {
            synchronized (this) {
                final CollectionChangeEventCreator c = this.createChildrenCreator();

                if (!fields.keySet().containsAll(this.getFieldsName())) {
                    for (String removed : CollectionUtils.substract(this.getFieldsName(), fields.keySet())) {
                        this.fields.remove(removed).dropped();
                    }
                }

                for (final SQLField newField : fields.values()) {
                    if (getChildrenNames().contains(newField.getName())) {
                        // re-use old instances by refreshing existing ones
                        this.getField(newField.getName()).mutateTo(newField);
                    } else {
                        final SQLField fieldToAdd;
                        // happens when the new structure is loaded in-memory
                        // before the current one is mutated to it
                        // (we already checked the fullname of the table)
                        if (newField.getTable() != this)
                            fieldToAdd = new SQLField(this, newField);
                        else
                            fieldToAdd = newField;
                        this.fields.put(newField.getName(), fieldToAdd);
                    }
                }

                // order matters (e.g. for indexes)
                final Set<SQLField> newPK = new LinkedHashSet<SQLField>();
                for (final String pk : primaryKeys)
                    newPK.add(this.getField(pk));
                this.primaryKeys = Collections.unmodifiableSet(newPK);
                this.primaryKey = primaryKeys.size() == 1 ? this.getField(primaryKeys.get(0)) : null;
                this.primaryKeyOK = primaryKeys.size() <= 1;

                this.keys = null;
                this.fieldsGroups = null;

                // don't fetch the ID now as it could be too early (e.g. we just created the table
                // but haven't inserted the undefined row)
                this.undefinedID = undef;
                this.fireChildrenChanged(c);
            }
        }
    }

    // *** getter

    synchronized void setType(String type) {
        this.type = type;
    }

    public synchronized final String getType() {
        return this.type;
    }

    synchronized void setComment(String comm) {
        this.comment = comm;
    }

    public synchronized final String getComment() {
        return this.comment;
    }

    public synchronized final Trigger getTrigger(String name) {
        return this.triggers.get(name);
    }

    public synchronized final Map<String, Trigger> getTriggers() {
        return Collections.unmodifiableMap(this.triggers);
    }

    /**
     * The constraints on this table.
     * 
     * @return the constraints or <code>null</code> if they couldn't be retrieved.
     */
    public synchronized final Set<Constraint> getAllConstraints() {
        return this.constraints == null ? null : Collections.unmodifiableSet(this.constraints);
    }

    /**
     * The CHECK and UNIQUE constraints on this table. This is useful since types
     * {@link ConstraintType#FOREIGN_KEY FOREIGN_KEY} and {@link ConstraintType#PRIMARY_KEY
     * PRIMARY_KEY} are already available through {@link #getForeignLinks()} and
     * {@link #getPrimaryKeys()} ; type {@link ConstraintType#DEFAULT DEFAULT} through
     * {@link SQLField#getDefaultValue()}.
     * 
     * @return the constraints or <code>null</code> if they couldn't be retrieved.
     */
    public synchronized final Set<Constraint> getConstraints() {
        if (this.constraints == null)
            return null;
        final Set<Constraint> res = new HashSet<Constraint>();
        for (final Constraint c : this.constraints) {
            if (c.getType() == ConstraintType.CHECK || c.getType() == ConstraintType.UNIQUE) {
                res.add(c);
            }
        }
        return res;
    }

    /**
     * Returns a specific constraint.
     * 
     * @param type type of constraint, e.g. {@link ConstraintType#UNIQUE}.
     * @param cols the fields names, e.g. ["NAME"].
     * @return the matching constraint, <code>null</code> if it cannot be found or if constraints
     *         couldn't be retrieved.
     */
    public synchronized final Constraint getConstraint(ConstraintType type, List<String> cols) {
        if (this.constraints == null)
            return null;
        for (final Constraint c : this.constraints) {
            if (c.getType() == type && c.getCols().equals(cols)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Whether rows of this table can be represented as SQLRow.
     * 
     * @return <code>true</code> if rows of this table can be represented as SQLRow.
     */
    public synchronized boolean isRowable() {
        return this.getPrimaryKeys().size() == 1 && Number.class.isAssignableFrom(this.getKey().getType().getJavaType());
    }

    public SQLSchema getSchema() {
        return (SQLSchema) this.getParent();
    }

    public SQLBase getBase() {
        return this.getSchema().getBase();
    }

    synchronized final String getVersion() {
        return this.version;
    }

    /**
     * Return the primary key of this table.
     * 
     * @return the field which is the key of this table, or <code>null</code> if it doesn't exist.
     * @throws IllegalStateException if there's more than one primary key.
     */
    @Override
    public synchronized SQLField getKey() {
        if (!this.primaryKeyOK)
            throw new IllegalStateException(this + " has more than 1 primary key: " + this.getPrimaryKeys());
        return this.primaryKey;
    }

    /**
     * Return the primary keys of this table.
     * 
     * @return the fields (SQLField) which are the keys of this table, can be empty.
     */
    public synchronized Set<SQLField> getPrimaryKeys() {
        return this.primaryKeys;
    }

    public final Set<Link> getForeignLinks() {
        return this.getDBSystemRoot().getGraph().getForeignLinks(this);
    }

    /**
     * Return the foreign keys of this table.
     * 
     * @return a Set of SQLField which are foreign keys of this table.
     */
    public Set<SQLField> getForeignKeys() {
        return this.getDBSystemRoot().getGraph().getForeignKeys(this);
    }

    public Set<String> getForeignKeysNames() {
        return DatabaseGraph.getNames(this.getForeignLinks());
    }

    public Set<List<SQLField>> getForeignKeysFields() {
        return this.getDBSystemRoot().getGraph().getForeignKeysFields(this);
    }

    public Set<SQLField> getForeignKeys(String foreignTable) {
        return this.getForeignKeys(this.getTable(foreignTable));
    }

    public Set<SQLField> getForeignKeys(SQLTable foreignTable) {
        return this.getDBSystemRoot().getGraph().getForeignFields(this, foreignTable);
    }

    public SQLTable getForeignTable(String foreignField) {
        return this.getField(foreignField).getForeignTable();
    }

    public SQLTable findReferentTable(String tableName) {
        return this.getDBSystemRoot().getGraph().findReferentTable(this, tableName);
    }

    /**
     * Renvoie toutes les clefs de cette table. C'est à dire les clefs primaires plus les clefs
     * externes.
     * 
     * @return toutes les clefs de cette table, can be empty.
     */
    public synchronized Set<SQLField> getKeys() {
        if (this.keys == null) {
            this.keys = this.getFields(VirtualFields.KEYS);
        }
        return this.keys;
    }

    @Immutable
    static public final class FieldGroup {
        private final String field;
        private final SQLKey key;

        private FieldGroup(final SQLKey key, final String field) {
            assert (key == null) != (field == null);
            this.key = key;
            this.field = key == null ? field : CollectionUtils.getSole(key.getFields());
        }

        /**
         * The key type for this group.
         * 
         * @return the key type, <code>null</code> for a simple field.
         */
        public final Type getKeyType() {
            if (this.key == null)
                return null;
            return this.key.getType();
        }

        /**
         * The key for this group.
         * 
         * @return the key, <code>null</code> for a simple field.
         */
        public final SQLKey getKey() {
            return this.key;
        }

        /**
         * The one and only field of this group.
         * 
         * @return the only field of this group, only <code>null</code> if this group is a
         *         {@link #getKey() key} with more than one field.
         */
        public String getSingleField() {
            return this.field;
        }

        public final List<String> getFields() {
            return this.key == null ? Arrays.asList(this.field) : this.key.getFields();
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + " " + (this.key != null ? this.key.toString() : this.field);
        }
    }

    /**
     * Return the fields grouped by key.
     * 
     * @return for each field of this table the matching group.
     */
    public synchronized Map<String, FieldGroup> getFieldGroups() {
        if (this.fieldsGroups == null) {
            final Map<String, FieldGroup> res = new LinkedHashMap<String, FieldGroup>();
            // set order
            for (final String field : this.getFieldsName()) {
                res.put(field, new FieldGroup(null, field));
            }
            for (final Link l : this.getForeignLinks()) {
                indexKey(res, SQLKey.createForeignKey(l));
            }
            final SQLKey pk = SQLKey.createPrimaryKey(this);
            if (pk != null)
                indexKey(res, pk);

            this.fieldsGroups = Collections.unmodifiableMap(res);
        }
        return this.fieldsGroups;
    }

    static private final void indexKey(final Map<String, FieldGroup> m, final SQLKey k) {
        final FieldGroup group = new FieldGroup(k, null);
        for (final String field : k.getFields()) {
            final FieldGroup previous = m.put(field, group);
            assert previous.getKeyType() == null;
        }
    }

    public String toString() {
        return "/" + this.getName() + "/";
    }

    /**
     * Return the field named <i>fieldName </i> in this table.
     * 
     * @param fieldName the name of the field.
     * @return the matching field, never <code>null</code>.
     * @throws IllegalArgumentException if the field is not in this table.
     * @see #getFieldRaw(String)
     */
    @Override
    public SQLField getField(String fieldName) {
        SQLField res = this.getFieldRaw(fieldName);
        if (res == null) {
            throw new IllegalArgumentException("unknown field " + fieldName + " in " + this.getName() + ". The table " + this.getName() + " contains the followins fields: " + this.getFieldsName());
        }
        return res;
    }

    /**
     * Return the field named <i>fieldName</i> in this table.
     * 
     * @param fieldName the name of the field.
     * @return the matching field or <code>null</code> if none exists.
     */
    public SQLField getFieldRaw(String fieldName) {
        return this.fields.get(fieldName);
    }

    /**
     * Return all the fields in this table.
     * 
     * @return a Set of the fields.
     */
    public Set<SQLField> getFields() {
        return new HashSet<SQLField>(this.fields.values());
    }

    /**
     * An immutable set of fields.
     * 
     * @author Sylvain
     */
    @Immutable
    static public final class VirtualFields {

        static public final VirtualFields ORDER = new VirtualFields(VirtualFieldPartition.ORDER);
        static public final VirtualFields ARCHIVE = new VirtualFields(VirtualFieldPartition.ARCHIVE);
        static public final VirtualFields METADATA = new VirtualFields(VirtualFieldPartition.METADATA);
        static public final VirtualFields PRIMARY_KEY = new VirtualFields(VirtualFieldPartition.PRIMARY_KEY);
        static public final VirtualFields FOREIGN_KEYS = new VirtualFields(VirtualFieldPartition.FOREIGN_KEYS);
        /**
         * All specific fields of this table without keys.
         */
        static public final VirtualFields LOCAL_CONTENT = new VirtualFields(VirtualFieldPartition.LOCAL_CONTENT);

        /**
         * {@link #LOCAL_CONTENT local content fields} with {@link #FOREIGN_KEYS}.
         */
        static public final VirtualFields CONTENT = LOCAL_CONTENT.union(FOREIGN_KEYS);
        /**
         * {@link #CONTENT content fields} with {@link #METADATA}.
         */
        static public final VirtualFields CONTENT_AND_METADATA = CONTENT.union(METADATA);
        /**
         * {@link #PRIMARY_KEY} with {@link #FOREIGN_KEYS}.
         */
        static public final VirtualFields KEYS = PRIMARY_KEY.union(FOREIGN_KEYS);
        static public final VirtualFields NONE = new VirtualFields(EnumSet.noneOf(VirtualFieldPartition.class));
        static public final VirtualFields ALL = new VirtualFields(EnumSet.allOf(VirtualFieldPartition.class));

        private final EnumSet<VirtualFieldPartition> set;

        // use constants above
        private VirtualFields(final VirtualFieldPartition single) {
            this(EnumSet.of(single));
        }

        // private since parameter is not copied
        private VirtualFields(final EnumSet<VirtualFieldPartition> set) {
            if (set == null)
                throw new NullPointerException("Null set");
            this.set = set;
        }

        public final VirtualFields union(VirtualFields... other) {
            final EnumSet<VirtualFieldPartition> set = this.set.clone();
            for (final VirtualFields o : other)
                set.addAll(o.set);
            return new VirtualFields(set);
        }

        public final VirtualFields intersection(VirtualFields... other) {
            final EnumSet<VirtualFieldPartition> set = this.set.clone();
            for (final VirtualFields o : other)
                set.retainAll(o.set);
            return new VirtualFields(set);
        }

        public final VirtualFields difference(VirtualFields... other) {
            final EnumSet<VirtualFieldPartition> set = this.set.clone();
            for (final VirtualFields o : other)
                set.removeAll(o.set);
            return new VirtualFields(set);
        }

        public final VirtualFields complement() {
            // optimizations
            if (this == ALL)
                return NONE;
            else if (this == NONE)
                return ALL;
            return new VirtualFields(EnumSet.complementOf(this.set));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            return prime + this.set.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final VirtualFields other = (VirtualFields) obj;
            return this.set.equals(other.set);
        }
    }

    /**
     * A partition of the fields (except that some can be empty). Being a partition allow to use
     * {@link EnumSet#complementOf(EnumSet)}.
     * 
     * @author Sylvain
     * @see VirtualFields
     */
    static public enum VirtualFieldPartition {
        ORDER {
            @Override
            Set<SQLField> getFields(SQLTable t) {
                final SQLField orderField = t.getOrderField();
                return orderField == null ? Collections.<SQLField> emptySet() : Collections.singleton(orderField);
            }
        },
        ARCHIVE {
            @Override
            Set<SQLField> getFields(SQLTable t) {
                final SQLField f = t.getArchiveField();
                return f == null ? Collections.<SQLField> emptySet() : Collections.singleton(f);
            }
        },
        METADATA {
            @Override
            Set<SQLField> getFields(SQLTable t) {
                final Set<SQLField> res = new HashSet<SQLField>(4);
                res.add(t.getCreationDateField());
                res.add(t.getCreationUserField());
                res.add(t.getModifDateField());
                res.add(t.getModifUserField());
                res.remove(null);
                return res;
            }
        },
        PRIMARY_KEY {
            @Override
            Set<SQLField> getFields(SQLTable t) {
                return t.getPrimaryKeys();
            }
        },
        FOREIGN_KEYS {
            @Override
            Set<SQLField> getFields(SQLTable t) {
                return DatabaseGraph.getColsUnion(t.getForeignLinks());
            }
        },
        /**
         * {@link #CONTENT content fields} without {@link #FOREIGN_KEYS}
         */
        LOCAL_CONTENT {
            @Override
            Set<SQLField> getFields(SQLTable t) {
                throw new IllegalStateException(this + " is any field not in another set");
            }
        };

        abstract Set<SQLField> getFields(final SQLTable t);
    }

    public final Set<SQLField> getFields(final VirtualFields vfs) {
        return getFields(vfs.set);
    }

    final Set<SQLField> getFields(final Set<VirtualFieldPartition> vf) {
        if (vf.isEmpty())
            return Collections.emptySet();

        final Set<SQLField> res;
        // LOCAL_CONTENT is just ALL minus every other set
        if (!vf.contains(VirtualFieldPartition.LOCAL_CONTENT)) {
            res = new HashSet<SQLField>();
            for (final VirtualFieldPartition v : vf) {
                res.addAll(v.getFields(this));
            }
        } else {
            res = this.getFields();
            // don't use EnumSet.complementOf(EnumSet.copyOf()) as it makes multiple copies
            for (final VirtualFieldPartition v : VirtualFieldPartition.values()) {
                if (!vf.contains(v)) {
                    res.removeAll(v.getFields(this));
                }
            }
        }
        return Collections.unmodifiableSet(res);
    }

    public final Set<String> getFieldsNames(final VirtualFields vfs) {
        final Set<String> res = new HashSet<String>();
        for (final SQLField f : this.getFields(vfs)) {
            res.add(f.getName());
        }
        return res;
    }

    public final List<SQLField> getFields(final Collection<String> names) {
        return this.getFields(names, new ArrayList<SQLField>());
    }

    public final <T extends Collection<SQLField>> T getFields(final Collection<String> names, final T res) {
        return this.getFields(names, res, true);
    }

    public final <T extends Collection<SQLField>> T getFields(final Collection<String> names, final T res, final boolean required) {
        for (final String name : names) {
            final SQLField f = required ? this.getField(name) : this.getFieldRaw(name);
            if (f != null)
                res.add(f);
        }
        return res;
    }

    /**
     * Retourne les champs du contenu de cette table. C'est à dire ni la clef primaire, ni les
     * champs d'archive et d'ordre.
     * 
     * @return les champs du contenu de cette table.
     * @see VirtualFields#CONTENT
     */
    public Set<SQLField> getContentFields() {
        return this.getContentFields(false);
    }

    public synchronized Set<SQLField> getContentFields(final boolean includeMetadata) {
        return this.getFields(includeMetadata ? VirtualFields.CONTENT_AND_METADATA : VirtualFields.CONTENT);
    }

    /**
     * Retourne les champs du contenu local de cette table. C'est à dire uniquement les champs du
     * contenu qui ne sont pas des clefs externes.
     * 
     * @return les champs du contenu local de cette table.
     * @see #getContentFields()
     */
    public synchronized Set<SQLField> getLocalContentFields() {
        return this.getFields(VirtualFields.LOCAL_CONTENT);
    }

    /**
     * Return the names of all the fields.
     * 
     * @return the names of all the fields.
     */
    public Set<String> getFieldsName() {
        return this.fields.keySet();
    }

    /**
     * Return all the fields in this table. The order is the same across reboot.
     * 
     * @return a List of the fields.
     */
    public List<SQLField> getOrderedFields() {
        return new ArrayList<SQLField>(this.fields.values());
    }

    @Override
    public Map<String, SQLField> getChildrenMap() {
        return this.fields.getImmutable();
    }

    public final SQLTable getTable(String name) {
        return this.getDesc(name, SQLTable.class);
    }

    /**
     * Retourne le nombre total de lignes contenues dans cette table.
     * 
     * @return le nombre de lignes de cette table.
     */
    public int getRowCount() {
        return this.getRowCount(true);
    }

    public int getRowCount(final boolean includeUndefined) {
        return this.getRowCount(includeUndefined, ArchiveMode.BOTH);
    }

    public int getRowCount(final boolean includeUndefined, final ArchiveMode archiveMode) {
        final SQLSelect sel = new SQLSelect(true).addSelectFunctionStar("count").addFrom(this);
        sel.setExcludeUndefined(!includeUndefined);
        sel.setArchivedPolicy(archiveMode);
        final Number count = (Number) this.getBase().getDataSource().execute(sel.asString(), new IResultSetHandler(SQLDataSource.SCALAR_HANDLER, false));
        return count.intValue();
    }

    /**
     * The maximum value of the order field.
     * 
     * @return the maximum value of the order field, or -1 if this table is empty.
     */
    public BigDecimal getMaxOrder() {
        return this.getMaxOrder(true);
    }

    public BigDecimal getMaxOrder(Boolean useCache) {
        final SQLField orderField = this.getOrderField();
        if (orderField == null)
            throw new IllegalStateException(this + " is not ordered");
        final SQLSelect sel = new SQLSelect(true).addSelect(orderField, "max");
        try {
            final BigDecimal maxOrder = (BigDecimal) this.getBase().getDataSource().execute(sel.asString(), new IResultSetHandler(SQLDataSource.SCALAR_HANDLER, useCache));
            return maxOrder == null ? BigDecimal.ONE.negate() : maxOrder;
        } catch (ClassCastException e) {
            throw new IllegalStateException(orderField.getSQLName() + " must be " + SQLSyntax.get(this).getOrderDefinition(), e);
        }
    }

    /**
     * Retourne la ligne correspondant à l'ID passé.
     * 
     * @param ID l'identifiant de la ligne à retourner.
     * @return une ligne existant dans la base sinon <code>null</code>.
     * @see #getValidRow(int)
     */
    public SQLRow getRow(int ID) {
        SQLRow row = this.getUncheckedRow(ID);
        return row.exists() ? row : null;
    }

    /**
     * Retourne une la ligne demandée sans faire aucune vérification.
     * 
     * @param ID l'identifiant de la ligne à retourner.
     * @return la ligne demandée, jamais <code>null</code>.
     */
    private SQLRow getUncheckedRow(int ID) {
        return new SQLRow(this, ID);
    }

    /**
     * Retourne la ligne valide correspondant à l'ID passé.
     * 
     * @param ID l'identifiant de la ligne à retourner.
     * @return une ligne existante et non archivée dans la base sinon <code>null</code>.
     * @see SQLRow#isValid()
     */
    public SQLRow getValidRow(int ID) {
        SQLRow row = this.getRow(ID);
        return row.isValid() ? row : null;
    }

    /**
     * Vérifie la validité de cet ID. C'est à dire qu'il existe une ligne non archivée avec cet ID,
     * dans cette table.
     * 
     * @param ID l'identifiant.
     * @return <code>null</code> si l'ID est valide, sinon une SQLRow qui est soit inexistante, soit
     *         archivée.
     */
    public SQLRow checkValidity(int ID) {
        final SQLRow row = SQLRow.createFromSelect(this, VirtualFields.PRIMARY_KEY.union(VirtualFields.ARCHIVE), ID, LockStrength.SHARE);
        // l'inverse de getValidRow()
        return row.isValid() ? null : row;
    }

    /**
     * Vérifie cette table est intègre. C'est à dire que toutes ses clefs externes pointent sur des
     * lignes existantes et non effacées. Cette méthode retourne une liste constituée de triplet :
     * SQLRow (la ligne incohérente), SQLField (le champ incohérent), SQLRow (la ligne invalide de
     * la table étrangère).
     * 
     * @return a list of inconsistencies or <code>null</code> if this table is not rowable.
     */
    public List<Tuple3<SQLRow, SQLField, SQLRow>> checkIntegrity() {
        final SQLField pk;
        final Set<SQLField> fks;
        synchronized (this) {
            if (!this.isRowable())
                return null;
            pk = this.getKey();
            fks = this.getForeignKeys();
        }

        final List<Tuple3<SQLRow, SQLField, SQLRow>> inconsistencies = new ArrayList<Tuple3<SQLRow, SQLField, SQLRow>>();
        // si on a pas de relation externe, c'est OK
        if (!fks.isEmpty()) {
            final SQLSelect sel = new SQLSelect();
            // on ne vérifie pas les lignes archivées mais l'indéfinie oui.
            sel.setExcludeUndefined(false);
            sel.addSelect(pk);
            sel.addAllSelect(fks);
            this.getBase().getDataSource().execute(sel.asString(), new ResultSetHandler() {
                public Object handle(ResultSet rs) throws SQLException {
                    while (rs.next()) {
                        for (final SQLField fk : fks) {
                            final SQLRow pb = SQLTable.this.checkValidity(fk.getName(), rs.getInt(fk.getFullName()));
                            if (pb != null) {
                                final SQLRow row = SQLTable.this.getRow(rs.getInt(pk.getFullName()));
                                inconsistencies.add(Tuple3.create(row, fk, pb));
                            }
                        }
                    }
                    // on s'en sert pas
                    return null;
                }
            });
        }

        return inconsistencies;
    }

    /**
     * Vérifie que l'on peut affecter <code>foreignID</code> au champ <code>foreignKey</code> de
     * cette table. C'est à dire vérifie que la table sur laquelle pointe <code>foreignKey</code>
     * contient bien une ligne d'ID <code>foreignID</code> et de plus qu'elle n'a pas été archivée.
     * 
     * @param foreignKey le nom du champ.
     * @param foreignID l'ID que l'on souhaite tester.
     * @return une SQLRow décrivant l'incohérence ou <code>null</code> sinon.
     * @throws IllegalArgumentException si le champ passé n'est pas une clef étrangère.
     * @see #checkValidity(int)
     */
    public SQLRow checkValidity(String foreignKey, int foreignID) {
        final SQLField fk = this.getField(foreignKey);
        final SQLTable foreignTable = this.getDBSystemRoot().getGraph().getForeignTable(fk);
        if (foreignTable == null)
            throw new IllegalArgumentException("Impossible de tester '" + foreignKey + "' avec " + foreignID + " dans " + this + ". Ce n'est pas une clef étrangère.");
        return foreignTable.checkValidity(foreignID);
    }

    public SQLRow checkValidity(String foreignKey, Number foreignID) {
        // NULL is valid
        if (foreignID == null)
            return null;
        else
            return this.checkValidity(foreignKey, foreignID.intValue());
    }

    public boolean isOrdered() {
        return this.getOrderField() != null;
    }

    public SQLField getOrderField() {
        return this.getFieldRaw(orderField);
    }

    /**
     * The number of fractional digits of the order field.
     * 
     * @return the number of fractional digits of the order field.
     */
    public final int getOrderDecimalDigits() {
        return this.getOrderField().getType().getDecimalDigits().intValue();
    }

    public final BigDecimal getOrderULP() {
        return BigDecimal.ONE.scaleByPowerOfTen(-this.getOrderDecimalDigits());
    }

    public boolean isArchivable() {
        return this.getArchiveField() != null;
    }

    public SQLField getArchiveField() {
        return this.getFieldRaw(archiveField);
    }

    public SQLField getCreationDateField() {
        return this.getFieldRaw("CREATION_DATE");
    }

    public SQLField getCreationUserField() {
        return this.getFieldRaw("ID_USER_COMMON_CREATE");
    }

    public SQLField getModifDateField() {
        return this.getFieldRaw("MODIFICATION_DATE");
    }

    public SQLField getModifUserField() {
        return this.getFieldRaw("ID_USER_COMMON_MODIFY");
    }

    /**
     * The id of this table which means empty. Tables that aren't rowable or which use NULL to
     * signify empty have no UNDEFINED_ID.
     * 
     * @return the empty id or {@link SQLRow#NONEXISTANT_ID} if this table has no UNDEFINED_ID.
     */
    public final int getUndefinedID() {
        return this.getUndefinedID(false).intValue();
    }

    // if false getUndefinedID() might contact the DB
    synchronized final boolean undefinedIDKnown() {
        return this.undefinedID != null;
    }

    /*
     * No longer save the undefined IDs. We mustn't search undefined IDs when loading structure
     * since the undefined rows might not yet be inserted. When getUndefinedID() was called, we used
     * to save the ID alongside the table structure with the new structure version. Which is wrong
     * since we haven't refreshed the table structure. One solution would be to create an undefined
     * ID version : when loading, as with the structure, we now have to check the saved version
     * against the one in the metadata table, but since FWK_UNDEFINED_ID is small and already
     * cached, we might as well simplify and forego the version altogether.
     */

    private final Integer getUndefinedID(final boolean internal) {
        Integer res = null;
        synchronized (this) {
            if (this.undefinedID != null)
                res = this.undefinedID;
        }
        if (res == null) {
            if (!internal && this.getSchema().isFetchAllUndefinedIDs()) {
                // init all undefined, MAYBE one request with UNION ALL
                for (final SQLTable sibling : this.getSchema().getTables()) {
                    Integer siblingRes = sibling.getUndefinedID(true);
                    assert siblingRes != null;
                    if (sibling == this)
                        res = siblingRes;
                }
            } else {
                res = this.fetchUndefID();
                synchronized (this) {
                    this.undefinedID = res;
                }
            }
        }
        assert this.undefinedIDKnown();
        return res;
    }

    public final Number getUndefinedIDNumber() {
        final int res = this.getUndefinedID();
        if (res == SQLRow.NONEXISTANT_ID)
            return null;
        else
            return res;
    }

    // static

    static private final String orderField = "ORDRE";
    static private final String archiveField = "ARCHIVE";

    // /////// ******** OLD CODE ********

    /*
     * Gestion des événements
     */

    public void addTableModifiedListener(SQLTableModifiedListener l) {
        this.addTableModifiedListener(new ListenerAndConfig(l, AFTER_TX_DEFAULT));
    }

    public void addTableModifiedListener(ListenerAndConfig l) {
        this.addTableModifiedListener(l, false);
    }

    public void addPremierTableModifiedListener(ListenerAndConfig l) {
        this.addTableModifiedListener(l, true);
    }

    private void addTableModifiedListener(ListenerAndConfig l, final boolean before) {
        synchronized (this.listenersMutex) {
            final List<ListenerAndConfig> newListeners = new ArrayList<ListenerAndConfig>(this.tableModifiedListeners.size() + 1);
            if (before)
                newListeners.add(l);
            newListeners.addAll(this.tableModifiedListeners);
            if (!before)
                newListeners.add(l);
            this.tableModifiedListeners = Collections.unmodifiableList(newListeners);
        }
    }

    public void removeTableModifiedListener(SQLTableModifiedListener l) {
        this.removeTableModifiedListener(new ListenerAndConfig(l, AFTER_TX_DEFAULT));
    }

    public void removeTableModifiedListener(ListenerAndConfig l) {
        synchronized (this.listenersMutex) {
            final List<ListenerAndConfig> newListeners = new ArrayList<ListenerAndConfig>(this.tableModifiedListeners);
            if (newListeners.remove(l))
                this.tableModifiedListeners = Collections.unmodifiableList(newListeners);
        }
    }

    private static final class BridgeListener implements SQLTableModifiedListener {

        private final SQLTableListener l;

        private BridgeListener(SQLTableListener l) {
            super();
            this.l = l;
        }

        @Override
        public void tableModified(SQLTableEvent evt) {
            final Mode mode = evt.getMode();
            if (mode == Mode.ROW_ADDED)
                this.l.rowAdded(evt.getTable(), evt.getId());
            else if (mode == Mode.ROW_UPDATED)
                this.l.rowModified(evt.getTable(), evt.getId());
            else if (mode == Mode.ROW_DELETED)
                this.l.rowDeleted(evt.getTable(), evt.getId());
        }

        @Override
        public int hashCode() {
            return this.l.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof BridgeListener && this.l.equals(((BridgeListener) obj).l);
        }
    }

    /**
     * Ajoute un listener sur cette table.
     * 
     * @param l the listener.
     * @deprecated use {@link #addTableModifiedListener(SQLTableModifiedListener)}
     */
    public void addTableListener(SQLTableListener l) {
        this.addTableModifiedListener(new BridgeListener(l));
    }

    public void removeTableListener(SQLTableListener l) {
        this.removeTableModifiedListener(new BridgeListener(l));
    }

    /**
     * Previent tous les listeners de la table qu'il y a eu une modification ou ajout si modif de
     * d'une ligne particuliere.
     * 
     * @param id -1 signifie tout est modifié.
     */
    public void fireTableModified(final int id) {
        this.fire(Mode.ROW_UPDATED, id);
    }

    public void fireRowAdded(final int id) {
        this.fire(Mode.ROW_ADDED, id);
    }

    public void fireRowDeleted(final int id) {
        this.fire(Mode.ROW_DELETED, id);
    }

    public void fireTableModified(final int id, Collection<String> fields) {
        this.fire(new SQLTableEvent(this, id, Mode.ROW_UPDATED, fields));
    }

    private void fire(final Mode mode, final int id) {
        this.fire(new SQLTableEvent(this, id, mode, null));
    }

    public final void fire(SQLTableEvent evt) {
        this.fireTableModified(evt);
    }

    // the listeners and the event that was notified to them
    static private class FireState extends Tuple2<List<ListenerAndConfig>, SQLTableEvent> {
        public FireState(final List<ListenerAndConfig> listeners, final SQLTableEvent evt) {
            super(listeners, evt);
        }

        private DispatchingState createDispatchingState(final Boolean callbackAfterTxListeners, final boolean oppositeEvt) {
            final List<SQLTableModifiedListener> listeners = new LinkedList<SQLTableModifiedListener>();
            for (final ListenerAndConfig l : get0()) {
                if (callbackAfterTxListeners == null || l.callOnlyAfterTx() == null || callbackAfterTxListeners == l.callOnlyAfterTx())
                    listeners.add(l.getListener());
            }
            return new DispatchingState(listeners, oppositeEvt ? get1().opposite() : get1());
        }
    }

    static private class DispatchingState extends Tuple2<Iterator<SQLTableModifiedListener>, SQLTableEvent> {
        public DispatchingState(final List<SQLTableModifiedListener> listeners, final SQLTableEvent evt) {
            super(listeners.iterator(), evt);
        }
    }

    static private final ThreadLocal<LinkedList<DispatchingState>> events = new ThreadLocal<LinkedList<DispatchingState>>() {
        @Override
        protected LinkedList<DispatchingState> initialValue() {
            return new LinkedList<DispatchingState>();
        }
    };

    // allow to maintain the dispatching of events in order when a listener itself fires an event
    static private void fireTableModified(DispatchingState newTuple) {
        final LinkedList<DispatchingState> linkedList = events.get();
        // add new event
        linkedList.addLast(newTuple);
        // process all pending events
        DispatchingState currentTuple;
        while ((currentTuple = linkedList.peekFirst()) != null) {
            final Iterator<SQLTableModifiedListener> iter = currentTuple.get0();
            final SQLTableEvent currentEvt = currentTuple.get1();
            while (iter.hasNext()) {
                final SQLTableModifiedListener l = iter.next();
                l.tableModified(currentEvt);
            }
            // not removeFirst() since the item might have been already removed
            linkedList.pollFirst();
        }
    }

    private void fireTableModified(final SQLTableEvent evt) {
        if (evt.getTable() != this)
            throw new IllegalArgumentException("Wrong table : " + this + " ; " + evt);
        final FireState fireState;
        final TransactionPoint point = evt.getTransactionPoint();
        final Boolean callbackAfterTxListeners;
        synchronized (this.listenersMutex) {
            // no need to copy since this.tableModifiedListeners is immutable
            fireState = new FireState(this.tableModifiedListeners, evt);
            if (point == null) {
                // call back every listener
                callbackAfterTxListeners = null;
            } else if (point.isActive()) {
                addFireStates(point, Collections.singleton(fireState));
                callbackAfterTxListeners = false;
                // to free DB resources, it is allowed to fire events after the transaction ended
            } else if (!point.wasCommitted()) {
                throw new IllegalStateException("Fire after an aborted transaction point");
            } else if (point.getSavePoint() != null) {
                addFireStates(point, Collections.singleton(fireState));
                callbackAfterTxListeners = false;
            } else {
                callbackAfterTxListeners = null;
            }
        }
        fireTableModified(fireState.createDispatchingState(callbackAfterTxListeners, false));
    }

    private void addFireStates(TransactionPoint point, final Collection<FireState> fireStates) {
        assert Thread.holdsLock(this.listenersMutex) : "Unsafe to access this.transactions";
        // if multiple save points are released before firing, we must go back to the still active
        // point
        while (!point.isActive())
            point = point.getPrevious();
        if (!this.transactions.containsKey(point))
            point.addListener(this.txListener);
        this.transactions.addAll(point, fireStates);
    }

    // a transaction was committed or aborted, we must either notify listeners that wanted the
    // transaction to commit, or re-notify the listeners that didn't want to wait
    protected void fireFromTransaction(final TransactionPoint point) {
        final boolean committed = point.wasCommitted();
        // if it's a released savePoint, add all our states to the previous point (and thus don't
        // fire now)
        final boolean releasedSavePoint = committed && point.getSavePoint() != null;
        final List<FireState> states;
        synchronized (this.listenersMutex) {
            states = this.transactions.remove(point);
            if (releasedSavePoint) {
                this.addFireStates(point, states);
            }
        }
        if (!releasedSavePoint) {
            final ListIterator<FireState> iter = CollectionUtils.getListIterator(states, !committed);
            while (iter.hasNext()) {
                final FireState state = iter.next();
                fireTableModified(state.createDispatchingState(committed, !committed));
            }
        }
    }

    public synchronized String toXML() {
        final StringBuilder sb = new StringBuilder(16000);
        sb.append("<table name=\"");
        sb.append(OUTPUTTER.escapeAttributeEntities(this.getName()));
        sb.append("\"");

        final String schemaName = this.getSchema().getName();
        if (schemaName != null) {
            sb.append(" schema=\"");
            sb.append(OUTPUTTER.escapeAttributeEntities(schemaName));
            sb.append('"');
        }

        SQLSchema.appendVersionAttr(this.version, sb);

        if (getType() != null) {
            sb.append(" type=\"");
            sb.append(OUTPUTTER.escapeAttributeEntities(getType()));
            sb.append('"');
        }

        sb.append(">\n");

        if (this.getComment() != null) {
            sb.append("<comment>");
            sb.append(OUTPUTTER.escapeElementEntities(this.getComment()));
            sb.append("</comment>\n");
        }
        for (SQLField field : this.fields.values()) {
            sb.append(field.toXML());
        }
        sb.append("<primary>\n");
        for (SQLField element : this.primaryKeys) {
            sb.append(element.toXML());
        }
        sb.append("</primary>\n");
        // avoid writing unneeded chars
        if (this.triggers.size() > 0) {
            sb.append("<triggers>\n");
            for (Trigger t : this.triggers.values()) {
                sb.append(t.toXML());
            }
            sb.append("</triggers>\n");
        }
        if (this.constraints != null) {
            sb.append("<constraints>\n");
            for (Constraint t : this.constraints) {
                sb.append(t.toXML());
            }
            sb.append("</constraints>\n");
        }
        sb.append("</table>");
        return sb.toString();
    }

    @Override
    public SQLTableModifiedListener createTableListener(final SQLDataListener l) {
        return new SQLTableModifiedListener() {
            @Override
            public void tableModified(SQLTableEvent evt) {
                l.dataChanged(evt);
            }
        };
    }

    @Override
    public SQLTable getTable() {
        return this;
    }

    @Override
    public String getAlias() {
        return getName();
    }

    @Override
    public String getSQL() {
        // always use fullname, otherwise must check the datasource's
        // default schema
        return getSQLName().quote();
    }

    public boolean equalsDesc(SQLTable o) {
        return this.equalsDesc(o, true) == null;
    }

    /**
     * Compare this table and its descendants. This do not compare undefinedID as it isn't part of
     * the structure per se.
     * 
     * @param o the table to compare.
     * @param compareName whether to also compare the name, useful for comparing 2 tables in the
     *        same schema.
     * @return <code>null</code> if attributes and children of this and <code>o</code> are equals,
     *         otherwise a String explaining the differences.
     */
    public String equalsDesc(SQLTable o, boolean compareName) {
        return this.equalsDesc(o, null, compareName);
    }

    // ATTN otherSystem can be null, meaning compare exactly (even if the system of this table and
    // the system of the other table do not support the same features and thus tables cannot be
    // equal)
    // if otherSystem isn't null, then this method is more lenient and return true if the two tables
    // are the closest possible. NOTE that otherSystem is not required to be the system of the other
    // table, it might be something else if the other table was loaded into a system different than
    // the one which created the dump.
    public synchronized String equalsDesc(SQLTable o, SQLSystem otherSystem, boolean compareName) {
        if (o == null)
            return "other table is null";
        final boolean name = !compareName || this.getName().equals(o.getName());
        if (!name)
            return "name unequal : " + this.getName() + " " + o.getName();
        // TODO triggers, but wait for the dumping of functions
        // which mean wait for psql 8.4 pg_get_functiondef()
        // if (this.getServer().getSQLSystem() == o.getServer().getSQLSystem()) {
        // if (!this.getTriggers().equals(o.getTriggers()))
        // return "triggers unequal : " + this.getTriggers() + " " + o.getTriggers();
        // } else {
        // if (!this.getTriggers().keySet().equals(o.getTriggers().keySet()))
        // return "triggers names unequal : " + this.getTriggers() + " " + o.getTriggers();
        // }
        final boolean checkComment = otherSystem == null || this.getServer().getSQLSystem().isTablesCommentSupported() && otherSystem.isTablesCommentSupported();
        if (checkComment && !CompareUtils.equals(this.getComment(), o.getComment()))
            return "comment unequal : " + SQLBase.quoteStringStd(this.getComment()) + " != " + SQLBase.quoteStringStd(o.getComment());
        return this.equalsChildren(o, otherSystem);
    }

    private synchronized String equalsChildren(SQLTable o, SQLSystem otherSystem) {
        if (!this.getChildrenNames().equals(o.getChildrenNames()))
            return "fields differences: " + this.getChildrenNames() + "\n" + o.getChildrenNames();

        final String noLink = equalsChildrenNoLink(o, otherSystem);
        if (noLink != null)
            return noLink;

        // foreign keys
        final Set<Link> thisLinks = this.getForeignLinks();
        final Set<Link> oLinks = o.getForeignLinks();
        if (thisLinks.size() != oLinks.size())
            return "different number of foreign keys " + thisLinks + " != " + oLinks;
        final SQLSystem thisSystem = this.getServer().getSQLSystem();
        for (final Link l : thisLinks) {
            final Link ol = o.getDBSystemRoot().getGraph().getForeignLink(o, l.getCols());
            if (ol == null)
                return "no foreign key for " + l.getLabel();
            final SQLName thisPath = l.getTarget().getContextualSQLName(this);
            final SQLName oPath = ol.getTarget().getContextualSQLName(o);
            if (thisPath.getItemCount() != oPath.getItemCount())
                return "unequal path size : " + thisPath + " != " + oPath;
            if (!thisPath.getName().equals(oPath.getName()))
                return "unequal referenced table name : " + thisPath.getName() + " != " + oPath.getName();
            if (!getRule(l.getUpdateRule(), thisSystem, otherSystem).equals(getRule(ol.getUpdateRule(), thisSystem, otherSystem)))
                return "unequal update rule for " + l + ": " + l.getUpdateRule() + " != " + ol.getUpdateRule();
            if (!getRule(l.getDeleteRule(), thisSystem, otherSystem).equals(getRule(ol.getDeleteRule(), thisSystem, otherSystem)))
                return "unequal delete rule for " + l + ": " + l.getDeleteRule() + " != " + ol.getDeleteRule();
        }

        final Set<Constraint> thisConstraints;
        final Set<Constraint> otherConstraints;
        try {
            final Tuple2<Set<Constraint>, Set<Index>> thisConstraintsAndIndexes = this.getConstraintsAndIndexes();
            final Tuple2<Set<Constraint>, Set<Index>> otherConstraintsAndIndexes = o.getConstraintsAndIndexes();
            // order irrelevant
            final Set<Index> thisIndexesSet = thisConstraintsAndIndexes.get1();
            final Set<Index> oIndexesSet = otherConstraintsAndIndexes.get1();
            if (!thisIndexesSet.equals(oIndexesSet))
                return "indexes differences: " + thisIndexesSet + "\n" + oIndexesSet;
            thisConstraints = thisConstraintsAndIndexes.get0();
            otherConstraints = otherConstraintsAndIndexes.get0();
        } catch (SQLException e) {
            // MAYBE fetch indexes with the rest to avoid exn now
            return "couldn't get indexes: " + ExceptionUtils.getStackTrace(e);
        }
        if (!CustomEquals.equals(thisConstraints, otherConstraints, otherSystem == null || otherSystem.equals(thisSystem) ? null : Constraint.getInterSystemHashStrategy()))
            return "constraints unequal : '" + thisConstraints + "' != '" + otherConstraints + "'";

        return null;
    }

    private final Tuple2<Set<Constraint>, Set<Index>> getConstraintsAndIndexes() throws SQLException {
        final Set<Constraint> thisConstraints;
        final Set<Index> thisIndexes;
        if (this.getServer().getSQLSystem() != SQLSystem.MSSQL) {
            thisConstraints = this.getConstraints();
            thisIndexes = new HashSet<Index>(this.getIndexes(true));
        } else {
            thisConstraints = new HashSet<Constraint>(this.getConstraints());
            thisIndexes = new HashSet<Index>();
            for (final Index i : this.getIndexes()) {
                final Value<String> where = i.getMSUniqueWhere();
                if (!where.hasValue()) {
                    // regular index
                    thisIndexes.add(i);
                } else if (where.getValue() == null) {
                    final Map<String, Object> map = new HashMap<String, Object>();
                    map.put("CONSTRAINT_NAME", i.getName());
                    map.put("CONSTRAINT_TYPE", "UNIQUE");
                    map.put("COLUMN_NAMES", i.getCols());
                    map.put("DEFINITION", null);
                    thisConstraints.add(new Constraint(this, map));
                } else {
                    // remove extra IS NOT NULL, but does *not* translate [ARCHIVE]=(0) into
                    // "ARCHIVE" = 0
                    thisIndexes.add(this.createUniqueIndex(i.getName(), i.getCols(), where.getValue()));
                }
            }
        }
        return Tuple2.create(thisConstraints, thisIndexes);
    }

    private final Rule getRule(Rule r, SQLSystem thisSystem, SQLSystem otherSystem) {
        // compare exactly
        if (otherSystem == null)
            return r;
        // see http://code.google.com/p/h2database/issues/detail?id=352
        if (r == Rule.NO_ACTION && (thisSystem == SQLSystem.H2 || otherSystem == SQLSystem.H2))
            return Rule.RESTRICT;
        else
            return r;
    }

    /**
     * Compare the fields of this table, ignoring foreign constraints.
     * 
     * @param o the table to compare.
     * @param otherSystem the system <code>o</code> originates from, can be <code>null</code>.
     * @return <code>null</code> if each fields of this exists in <code>o</code> and is equal to it.
     */
    public synchronized final String equalsChildrenNoLink(SQLTable o, SQLSystem otherSystem) {
        for (final SQLField f : this.getFields()) {
            final SQLField oField = o.getField(f.getName());
            final boolean isPrimary = this.getPrimaryKeys().contains(f);
            if (isPrimary != o.getPrimaryKeys().contains(oField))
                return f + " is a primary not in " + o.getPrimaryKeys();
            final String equalsDesc = f.equalsDesc(oField, otherSystem, !isPrimary);
            if (equalsDesc != null)
                return equalsDesc;
        }
        return null;
    }

    public final SQLCreateMoveableTable getCreateTable() {
        return this.getCreateTable(SQLSyntax.get(this));
    }

    public synchronized final SQLCreateMoveableTable getCreateTable(final SQLSyntax syntax) {
        final SQLSystem system = syntax.getSystem();
        final SQLCreateMoveableTable res = new SQLCreateMoveableTable(syntax, this.getDBRoot().getName(), this.getName());
        for (final SQLField f : this.getOrderedFields()) {
            res.addColumn(f);
        }
        // primary keys
        res.setPrimaryKey(getPKsNames());
        // foreign keys
        for (final Link l : this.getForeignLinks())
            // don't generate explicit CREATE INDEX for fk, we generate all indexes below
            // (this also avoid creating a fk index that wasn't there)
            res.addForeignConstraint(l, false);
        // constraints
        if (this.constraints != null)
            for (final Constraint added : this.getConstraints()) {
                if (added.getType() == ConstraintType.UNIQUE) {
                    res.addUniqueConstraint(added.getName(), added.getCols());
                } else
                    throw new UnsupportedOperationException("unsupported constraint: " + added);
            }
        // indexes
        try {
            // MS unique constraint are not standard so we're forced to create indexes "where col is
            // not null" in addUniqueConstraint(). Thus when converting to another system we must
            // parse indexes to recreate actual constraints.
            final boolean convertMSIndex = this.getServer().getSQLSystem() == SQLSystem.MSSQL && system != SQLSystem.MSSQL;
            for (final Index i : this.getIndexes(true)) {
                Value<String> msWhere = null;
                if (convertMSIndex && (msWhere = i.getMSUniqueWhere()).hasValue()) {
                    if (msWhere.getValue() != null)
                        Log.get().warning("MS filter might not be valid in " + system + " : " + msWhere.getValue());
                    res.addUniqueConstraint(i.getName(), i.getCols(), msWhere.getValue());
                } else {
                    // partial unique index sometimes cannot be handled natively by the DB system
                    if (i.isUnique() && i.getFilter() != null && !system.isIndexFilterConditionSupported())
                        res.addUniqueConstraint(i.getName(), i.getCols(), i.getFilter());
                    else
                        res.addIndex(i);
                }
            }
        } catch (SQLException e) {
            // MAYBE fetch indexes with the rest to avoid exn now
            throw new IllegalStateException("could not get indexes", e);
        }
        // TODO triggers, but they are system dependent and we would have to parse the SQL
        // definitions to replace the different root/table name in DeferredClause.asString()
        if (this.getComment() != null)
            res.addOutsideClause(syntax.getSetTableComment(getComment()));
        return res;
    }

    public final List<String> getPKsNames() {
        return this.getPKsNames(new ArrayList<String>());
    }

    public synchronized final <C extends Collection<String>> C getPKsNames(C pks) {
        for (final SQLField f : this.getPrimaryKeys()) {
            pks.add(f.getName());
        }
        return pks;
    }

    public final String[] getPKsNamesArray() {
        return getPKsNames().toArray(new String[0]);
    }

    /**
     * Return the indexes mapped by column names. Ie a key will have as value every index that
     * mentions it, and a multi-column index will be in several entries. Note: this is not robust
     * since {@link Index#getCols()} isn't.
     * 
     * @return the indexes mapped by column names.
     * @throws SQLException if an error occurs.
     */
    public final SetMap<String, Index> getIndexesByField() throws SQLException {
        final List<Index> indexes = this.getIndexes();
        final SetMap<String, Index> res = new SetMap<String, Index>(indexes.size()) {
            @Override
            public Set<Index> createCollection(Collection<? extends Index> v) {
                final HashSet<Index> res = new HashSet<Index>(4);
                res.addAll(v);
                return res;
            }
        };
        for (final Index i : indexes)
            for (final String col : i.getCols())
                res.add(col, i);
        return res;
    }

    /**
     * Return the indexes on the passed columns names. Note: this is not robust since
     * {@link Index#getCols()} isn't.
     * 
     * @param cols fields names.
     * @return the matching indexes.
     * @throws SQLException if an error occurs.
     */
    public final List<Index> getIndexes(final List<String> cols) throws SQLException {
        final List<Index> res = new ArrayList<Index>();
        for (final Index i : this.getIndexes())
            if (i.getCols().equals(cols))
                res.add(i);
        return res;
    }

    /**
     * Return the indexes of this table. Except the primary key as every system generates it
     * automatically.
     * 
     * @return the list of indexes.
     * @throws SQLException if an error occurs while accessing the DB.
     */
    public final List<Index> getIndexes() throws SQLException {
        return this.getIndexes(false);
    }

    public synchronized final List<Index> getIndexes(final boolean normalized) throws SQLException {
        // in pg, a unique constraint creates a unique index that is not removeable
        // (except of course if we drop the constraint)
        // in mysql unique constraints and indexes are one and the same thing
        // so we must return them only in one (either getConstraints() or getIndexes())
        // anyway in all systems, a unique constraint or index achieve the same function
        // and so only generates the constraint and not the index
        final Set<List<String>> uniqConstraints;
        if (this.constraints != null) {
            uniqConstraints = new HashSet<List<String>>();
            for (final Constraint c : this.constraints) {
                if (c.getType() == ConstraintType.UNIQUE)
                    uniqConstraints.add(c.getCols());
            }
        } else
            uniqConstraints = Collections.emptySet();

        final List<Index> indexes = new ArrayList<Index>();
        Index currentIndex = null;
        for (final Map<String, Object> norm : this.getDBSystemRoot().getSyntax().getIndexInfo(this)) {
            final Index index = new Index(norm);
            final short seq = ((Number) norm.get("ORDINAL_POSITION")).shortValue();
            if (seq == 1) {
                if (canAdd(currentIndex, uniqConstraints))
                    indexes.add(currentIndex);
                currentIndex = index;
            } else {
                // continuing a multi-field index
                currentIndex.add(index);
            }
        }
        if (canAdd(currentIndex, uniqConstraints))
            indexes.add(currentIndex);

        if (normalized) {
            indexes.addAll(this.getPartialUniqueIndexes());
        }

        // MAYBE another request to find out index.getMethod() (eg pg.getIndexesReq())
        return indexes;
    }

    private boolean canAdd(final Index currentIndex, final Set<List<String>> uniqConstraints) {
        if (currentIndex == null || currentIndex.isPKIndex())
            return false;

        return !currentIndex.isUnique() || !uniqConstraints.contains(currentIndex.getCols());
    }

    // MAYBE inline
    protected synchronized final List<Index> getPartialUniqueIndexes() throws SQLException {
        final SQLSystem thisSystem = this.getServer().getSQLSystem();
        final List<Index> indexes = new ArrayList<Index>();
        // parse triggers, TODO remove them from triggers to output in getCreateTable()
        if (thisSystem == SQLSystem.H2) {
            for (final Trigger t : this.triggers.values()) {
                final Matcher matcher = ChangeTable.H2_UNIQUE_TRIGGER_PATTERN.matcher(t.getSQL());
                if (matcher.find()) {
                    final String indexName = ChangeTable.getIndexName(t.getName(), thisSystem);
                    final String[] javaCols = ChangeTable.H2_LIST_PATTERN.split(matcher.group(1).trim());
                    final List<String> cols = new ArrayList<String>(javaCols.length);
                    for (final String javaCol : javaCols) {
                        cols.add(StringUtils.unDoubleQuote(javaCol));
                    }
                    final String where = StringUtils.unDoubleQuote(matcher.group(2).trim());
                    indexes.add(createUniqueIndex(indexName, cols, where));
                }
            }
        } else if (thisSystem == SQLSystem.MYSQL) {
            for (final Trigger t : this.triggers.values()) {
                if (t.getAction().contains(ChangeTable.MYSQL_TRIGGER_EXCEPTION)) {
                    final String indexName = ChangeTable.getIndexName(t.getName(), thisSystem);
                    // MySQL needs a pair of triggers
                    final Trigger t2 = indexName == null ? null : this.triggers.get(indexName + ChangeTable.MYSQL_TRIGGER_SUFFIX_2);
                    // and their body must match
                    if (t2 != null && t2.getAction().equals(t.getAction())) {
                        final Matcher matcher = ChangeTable.MYSQL_UNIQUE_TRIGGER_PATTERN.matcher(t.getAction());
                        if (!matcher.find())
                            throw new IllegalStateException("Couldn't parse " + t.getAction());
                        // parse table name
                        final SQLName parsedName = SQLName.parse(matcher.group(1).trim());
                        if (!this.getName().equals(parsedName.getName()))
                            throw new IllegalStateException("Name mismatch : " + this.getSQLName() + " != " + parsedName);

                        final String[] wheres = ChangeTable.MYSQL_WHERE_PATTERN.split(matcher.group(2).trim());
                        final String userWhere = wheres[0];

                        final List<String> cols = new ArrayList<String>(wheres.length - 1);
                        for (int i = 1; i < wheres.length; i++) {
                            final Matcher eqMatcher = ChangeTable.MYSQL_WHERE_EQ_PATTERN.matcher(wheres[i].trim());
                            if (!eqMatcher.matches())
                                throw new IllegalStateException("Invalid where clause " + wheres[i]);
                            cols.add(SQLName.parse(eqMatcher.group(2).trim()).getName());
                        }
                        if (cols.isEmpty())
                            throw new IllegalStateException("No columns in " + Arrays.asList(wheres));
                        indexes.add(createUniqueIndex(indexName, cols, userWhere));
                    }
                }
            }
        }
        return indexes;
    }

    public static class SQLIndex {

        private static final Pattern NORMALIZE_SPACES = Pattern.compile("\\s+");

        private final String name;
        // SQL, e.g. : lower("name"), "age"
        private final List<String> attrs;
        private final boolean unique;
        private String method;
        private final String filter;

        public SQLIndex(final String name, final List<String> attributes, final boolean unique, final String filter) {
            this(name, attributes, false, unique, filter);
        }

        public SQLIndex(final String name, final List<String> attributes, final boolean quoteAll, final boolean unique, final String filter) {
            super();
            this.name = name;
            this.attrs = new ArrayList<String>(attributes.size());
            for (final String attr : attributes)
                this.addAttr(quoteAll ? SQLBase.quoteIdentifier(attr) : attr);
            this.unique = unique;
            this.method = null;
            // helps when comparing
            this.filter = filter == null ? null : NORMALIZE_SPACES.matcher(filter.trim()).replaceAll(" ");
        }

        public final String getName() {
            return this.name;
        }

        public final boolean isUnique() {
            return this.unique;
        }

        /**
         * All attributes forming this index.
         * 
         * @return the components of this index, eg ["lower(name)", "age"].
         */
        public final List<String> getAttrs() {
            return Collections.unmodifiableList(this.attrs);
        }

        protected final void addAttr(final String attr) {
            this.attrs.add(attr);
        }

        public final void setMethod(String method) {
            this.method = method;
        }

        public final String getMethod() {
            return this.method;
        }

        /**
         * Filter for partial index.
         * 
         * @return the where clause or <code>null</code>.
         */
        public final String getFilter() {
            return this.filter;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " " + this.getName() + " unique: " + this.isUnique() + " cols: " + this.getAttrs() + " filter: " + this.getFilter();
        }

        // ATTN don't use name since it is often auto-generated (eg by a UNIQUE field)
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SQLIndex) {
                final SQLIndex o = (SQLIndex) obj;
                return this.isUnique() == o.isUnique() && this.getAttrs().equals(o.getAttrs()) && CompareUtils.equals(this.getFilter(), o.getFilter())
                        && CompareUtils.equals(this.getMethod(), o.getMethod());
            } else {
                return false;
            }
        }

        // ATTN use cols, so use only after cols are done
        @Override
        public int hashCode() {
            return this.getAttrs().hashCode() + ((Boolean) this.isUnique()).hashCode();
        }
    }

    private final Index createUniqueIndex(final String name, final List<String> cols, final String where) {
        final Index res = new Index(name, cols.get(0), false, where);
        for (int i = 1; i < cols.size(); i++) {
            res.addFromMD(cols.get(i));
        }
        return res;
    }

    private final String removeParens(String filter) {
        if (filter != null) {
            filter = filter.trim();
            final SQLSystem sys = this.getServer().getSQLSystem();
            // postgreSQL always wrap filter with parens, ATTN we shouldn't remove from
            // "(A) and (B)" but still support "(A = (0))"
            if ((sys == SQLSystem.POSTGRESQL || sys == SQLSystem.MSSQL) && filter.startsWith("(") && filter.endsWith(")")) {
                filter = filter.substring(1, filter.length() - 1);
            }
        }
        return filter;
    }

    public final class Index extends SQLIndex {

        private final List<String> cols;

        Index(final Map<String, Object> row) {
            this((String) row.get("INDEX_NAME"), (String) row.get("COLUMN_NAME"), (Boolean) row.get("NON_UNIQUE"), (String) row.get("FILTER_CONDITION"));
        }

        Index(final String name, String col, Boolean nonUnique, String filter) {
            super(name, Collections.<String> emptyList(), !nonUnique, removeParens(filter));
            this.cols = new ArrayList<String>();
            this.addFromMD(col);
        }

        public final SQLTable getTable() {
            return SQLTable.this;
        }

        /**
         * The table columns in this index. Note that due to DB system limitation this list is
         * incomplete (e.g. missing expressions).
         * 
         * @return the unquoted columns, e.g. ["age"].
         */
        public final List<String> getCols() {
            return this.cols;
        }

        public final List<SQLField> getFields() {
            final List<SQLField> res = new ArrayList<SQLField>(this.getCols().size());
            for (final String f : this.getCols())
                res.add(getTable().getField(f));
            return res;
        }

        /**
         * Adds a column to this multi-field index.
         * 
         * @param name the name of the index.
         * @param col the column to add.
         * @param unique whether the index is unique.
         * @throws IllegalStateException if <code>name</code> and <code>unique</code> are not the
         *         same as these.
         */
        private final void add(final Index o) {
            assert o.getAttrs().size() == 1;
            if (!o.getName().equals(this.getName()) || this.isUnique() != o.isUnique())
                throw new IllegalStateException("incoherence");
            this.cols.addAll(o.getCols());
            this.addAttr(o.getAttrs().get(0));
        }

        // col is either an expression or a column name
        protected void addFromMD(String col) {
            if (getTable().contains(col)) {
                // e.g. age
                this.cols.add(col);
                this.addAttr(SQLBase.quoteIdentifier(col));
            } else {
                // e.g. lower("name")
                this.addAttr(col);
            }
        }

        final boolean isPKIndex() {
            return this.isUnique() && this.getCols().equals(getTable().getPKsNames()) && this.getCols().size() == this.getAttrs().size();
        }

        private final Pattern getColPattern(final String col) {
            // e.g. ([NOM] IS NOT NULL AND [PRENOM] IS NOT NULL AND [ARCHIVE]=(0))
            return Pattern.compile("(?i:\\s+AND\\s+)?" + Pattern.quote(new SQLName(col).quoteMS()) + "\\s+(?i)IS\\s+NOT\\s+NULL(\\s+AND\\s+)?");
        }

        // in MS SQL we're forced to add IS NOT NULL to get the standard behaviour
        // return none if it's not a unique index, otherwise the value of the where for the partial
        // index (can be null)
        final Value<String> getMSUniqueWhere() {
            assert getServer().getSQLSystem() == SQLSystem.MSSQL;
            if (this.isUnique() && this.getFilter() != null) {
                String filter = this.getFilter().trim();
                // for each column, remove its NOT NULL clause
                for (final String col : getCols()) {
                    final Matcher matcher = this.getColPattern(col).matcher(filter);
                    if (matcher.find()) {
                        filter = matcher.replaceFirst("").trim();
                    } else {
                        return Value.getNone();
                    }
                }
                // what is the left is the actual filter
                filter = filter.trim();
                return Value.getSome(filter.isEmpty() ? null : filter);
            }
            return Value.getNone();
        }
    }
}
