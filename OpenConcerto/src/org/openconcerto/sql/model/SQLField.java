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
 
 /*
 * Field created on 4 mai 2004
 */
package org.openconcerto.sql.model;

import static org.openconcerto.sql.model.SQLBase.quoteIdentifier;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.SQLTable.FieldGroup;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.SQLKey.Type;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.Value;
import org.openconcerto.xml.JDOM2Utils;
import org.openconcerto.xml.XMLCodecUtils;

import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Element;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * Un champ SQL. Pour obtenir une instance de cette classe il faut utiliser
 * {@link SQLTable#getField(String)}. Un champ connait sa table, son nom, son type et sa valeur par
 * défaut.
 * 
 * @author ILM Informatique 4 mai 2004
 */
@ThreadSafe
public class SQLField extends SQLIdentifier implements FieldRef, IFieldPath {

    static final char CHAR = '|';

    // nextVal('"SCHEMA"."seqName"'::regclass);
    static private final Pattern SEQ_PATTERN = Pattern.compile("nextval\\('(.+)'.*\\)");

    static final SQLField create(SQLTable t, ResultSet rs) throws SQLException {
        final SQLSystem system = t.getServer().getSQLSystem();
        final String fieldName = rs.getString("COLUMN_NAME");

        final int dataType = rs.getInt("DATA_TYPE");
        final int size = rs.getInt("COLUMN_SIZE");
        final SQLType type;

        try {
            // MS doesn't return an int
            final Object decDig = rs.getObject("DECIMAL_DIGITS");
            final Integer intDecDig = (Integer) (decDig == null || decDig instanceof Integer ? decDig : ((Number) decDig).intValue());
            String typeName = rs.getString("TYPE_NAME");
            if (system == SQLSystem.POSTGRESQL) {
                // AbstractJdbc2DatabaseMetaData.getColumns() convert the true type to serial. But
                // the default value is kept, which is redundant. Further in the framework we need
                // to know the true type (e.g. to cast), and there's SQLSyntax.isAuto()/getAuto() to
                // handle serial.
                if (typeName.toLowerCase().equals("bigserial"))
                    typeName = "int8";
                else if (typeName.toLowerCase().equals("serial"))
                    typeName = "int4";
            }
            type = SQLType.get(t.getBase(), dataType, size, intDecDig, typeName);
        } catch (IllegalStateException e) {
            throw ExceptionUtils.createExn(IllegalStateException.class, "can't create " + t + " " + fieldName, e);
        }

        final Map<String, Object> map;
        // MS sql throws an exception for rs.getObject("IS_AUTOINCREMENT") :
        // La conversion de char en SMALLINT n'est pas prise en charge.
        if (system == SQLSystem.MSSQL) {
            map = SQLDataSource.ROW_PROC.toMap(rs, Collections.singleton("IS_AUTOINCREMENT"));
            // get*(String) is costly so only use it for MS
            map.put("IS_AUTOINCREMENT", rs.getString("IS_AUTOINCREMENT"));
        } else {
            map = SQLDataSource.ROW_PROC.toMap(rs);
        }

        return new SQLField(t, fieldName, type, map);
    }

    static private Boolean nullableStr2Obj(final String isNullable) {
        final Boolean res;
        if ("YES".equalsIgnoreCase(isNullable))
            res = Boolean.TRUE;
        else if ("NO".equalsIgnoreCase(isNullable))
            res = Boolean.FALSE;
        else
            res = null;
        return res;
    }

    @SuppressWarnings("unchecked")
    static SQLField create(SQLTable t, Element elementField) {
        final String fieldName = elementField.getAttributeValue("name");

        SQLType type = SQLType.get(t.getBase(), elementField.getChild("type"));
        final Map<String, Object> metadata = (Map<String, Object>) XMLCodecUtils.decode1(elementField.getChild("java"));
        final Map<String, Object> infoSchema = (Map<String, Object>) XMLCodecUtils.decode1(elementField.getChild("infoSchema").getChild("java"));

        final SQLField res = new SQLField(t, fieldName, type, metadata);
        res.setColsFromInfoSchema(infoSchema);
        return res;
    }

    /**
     * Properties of a field.
     * 
     * @author Sylvain
     */
    public static enum Properties {
        NAME, TYPE, DEFAULT, NULLABLE
    };

    private final String fullName;

    // all following attributes guarded by "this"
    private SQLType type;
    private final Map<String, Object> metadata;
    private String defaultValue;
    @GuardedBy("this")
    private Value<Object> parsedDefaultValue;
    private Boolean nullable;
    // from information_schema.COLUMNS
    private final Map<String, Object> infoSchemaCols;

    private String xml;

    SQLField(SQLTable table, String name, SQLType type, Map<String, Object> metadata) {
        super(table, name);
        this.type = type;
        this.metadata = metadata;
        // quite a few entries have null values, remove them since we don't use keys
        // and this take a decent amount of space when saved as XML
        final Iterator<Entry<String, Object>> iter = this.metadata.entrySet().iterator();
        while (iter.hasNext()) {
            final Entry<String, Object> e = iter.next();
            if (e.getValue() == null)
                iter.remove();
        }
        // pg jdbc use pg_catalog.pg_attrdef.adsrc (see
        // org.postgresql.jdbc2.AbstractJdbc2DatabaseMetaData#getColumns()) but should use
        // pg_get_expr(adbin) (see 44.6. pg_attrdef), this sometimes result in
        // <nextval('"Preventec_Common"."DISCIPLINE_ID_seq"'::regclass)> !=
        // <nextval('"DISCIPLINE_ID_seq"'::regclass)>
        this.defaultValue = (String) metadata.get("COLUMN_DEF");
        // don't parse now, as it might not be possible : i.e. function calls
        this.parsedDefaultValue = null;
        this.fullName = this.getTable().getName() + "." + this.getName();
        this.nullable = nullableStr2Obj((String) metadata.get("IS_NULLABLE"));

        this.infoSchemaCols = new HashMap<String, Object>();

        this.xml = null;
    }

    SQLField(SQLTable table, SQLField f) {
        super(table, f.getName());
        this.type = f.type;
        this.metadata = new HashMap<String, Object>(f.metadata);
        this.defaultValue = f.defaultValue;
        this.parsedDefaultValue = f.parsedDefaultValue;
        this.fullName = f.fullName;
        this.nullable = f.nullable;
        this.infoSchemaCols = new HashMap<String, Object>(f.infoSchemaCols);
        this.xml = f.xml;
    }

    synchronized void mutateTo(SQLField f) {
        if (this == f)
            return;
        this.type = f.type;
        this.metadata.clear();
        this.metadata.putAll(f.metadata);
        this.defaultValue = f.defaultValue;
        this.parsedDefaultValue = f.parsedDefaultValue;
        this.nullable = f.nullable;
        this.setColsFromInfoSchema(f.infoSchemaCols);
        this.xml = f.xml;
    }

    @SuppressWarnings("unchecked")
    synchronized void setColsFromInfoSchema(Map m) {
        this.infoSchemaCols.clear();
        this.infoSchemaCols.putAll(m);
        this.infoSchemaCols.keySet().removeAll(SQLSyntax.INFO_SCHEMA_NAMES_KEYS);
    }

    @Override
    public String toString() {
        return CHAR + this.getFullName() + CHAR;
    }

    /**
     * Le nom complet de ce champ.
     * 
     * @return le nom complet de ce champ, ie NOM_TABLE.NOM_CHAMP.
     */
    public synchronized final String getFullName() {
        return this.fullName;
    }

    public SQLTable getTable() {
        return (SQLTable) this.getParent();
    }

    public synchronized SQLType getType() {
        return this.type;
    }

    /**
     * Return the type of this field in SQL.
     * 
     * @return the SQL for the type, e.g. "int" or "decimal(16,8)".
     * @see SQLSyntax#getType(SQLField)
     */
    public final String getTypeDecl() {
        return this.getDBSystemRoot().getSyntax().getType(this);
    }

    /**
     * Metadata from JDBC.
     * 
     * @param name metadata name, eg "DECIMAL_DIGITS".
     * @return the value.
     * @see DatabaseMetaData#getColumns(String, String, String, String)
     */
    public synchronized Object getMetadata(String name) {
        return this.metadata.get(name);
    }

    /**
     * Additional metadata from INFORMATION_SCHEMA.
     * 
     * @return metadata from INFORMATION_SCHEMA.
     * @see #getMetadata(String)
     */
    public synchronized final Map<String, Object> getInfoSchema() {
        return Collections.unmodifiableMap(this.infoSchemaCols);
    }

    /**
     * The sequence linked to this field. I.e. that sequence will be dropped if this field is.
     * 
     * @return the quoted name of the sequence, <code>null</code> if none.
     */
    public final SQLName getOwnedSequence() {
        return this.getOwnedSequence(false);
    }

    public final SQLName getOwnedSequence(final boolean allowRequest) {
        final SQLSystem sys = getServer().getSQLSystem();
        if (sys == SQLSystem.H2) {
            final String name = (String) this.infoSchemaCols.get("SEQUENCE_NAME");
            if (name != null) {
                // H2 doesn't provide the schema name, but requires it when altering a field
                return new SQLName(getDBRoot().getName(), name);
            }
        } else if (sys == SQLSystem.POSTGRESQL) {
            if (allowRequest) {
                final String req = "SELECT pg_get_serial_sequence(" + getTable().getBase().quoteString(getTable().getSQLName().quote()) + ", " + getTable().getBase().quoteString(this.getName()) + ")";
                final String name = (String) getDBSystemRoot().getDataSource().executeScalar(req);
                if (name != null)
                    return SQLName.parse(name);
            } else if (this.getDefaultValue() != null) {
                final String def = this.getDefaultValue().trim();
                if (def.startsWith("nextval")) {
                    final Matcher matcher = SEQ_PATTERN.matcher(def);
                    if (matcher.matches()) {
                        return SQLName.parse(matcher.group(1));
                    } else {
                        throw new IllegalStateException("could not parse: " + def + " with " + SEQ_PATTERN.pattern());
                    }
                }
            }
        }
        return null;
    }

    /**
     * The SQL default value.
     * 
     * @return the default value, e.g. <code>"1"</code> or <code>"'none'"</code>.
     * @see DatabaseMetaData#getColumns(String, String, String, String)
     */
    public synchronized String getDefaultValue() {
        return this.defaultValue;
    }

    /**
     * Try to parse the SQL {@link #getDefaultValue() default value}. Numbers are always parsed to
     * {@link BigDecimal}.
     * 
     * @return {@link Value#getNone()} if parsing failed, otherwise the parsed value.
     */
    public synchronized Value<Object> getParsedDefaultValue() {
        if (this.parsedDefaultValue == null) {
            final Class<?> javaType = this.getType().getJavaType();
            final String defaultVal = SQLSyntax.getNormalizedDefault(this);
            try {
                Object p = null;
                if (defaultVal == null || defaultVal.trim().equalsIgnoreCase("null")) {
                    p = null;
                } else if (String.class.isAssignableFrom(javaType)) {
                    // Strings can be encoded a lot of different ways, see SQLBase.quoteString()
                    if (defaultVal.charAt(0) == '\'' && defaultVal.indexOf('\\') == -1)
                        p = SQLBase.unquoteStringStd(defaultVal);
                    else
                        this.parsedDefaultValue = Value.getNone();
                } else if (Number.class.isAssignableFrom(javaType)) {
                    p = new BigDecimal(defaultVal);
                } else if (Boolean.class.isAssignableFrom(javaType)) {
                    p = Boolean.parseBoolean(defaultVal);
                } else if (Timestamp.class.isAssignableFrom(javaType)) {
                    p = Timestamp.valueOf(SQLBase.unquoteStringStd(defaultVal));
                } else if (Time.class.isAssignableFrom(javaType)) {
                    p = Time.valueOf(SQLBase.unquoteStringStd(defaultVal));
                } else if (Date.class.isAssignableFrom(javaType)) {
                    p = java.sql.Date.valueOf(SQLBase.unquoteStringStd(defaultVal));
                } else {
                    throw new IllegalStateException("Unsupported type " + this.getType());
                }
                if (this.parsedDefaultValue == null)
                    this.parsedDefaultValue = Value.<Object> getSome(p);
            } catch (Exception e) {
                Log.get().log(Level.FINE, "Couldn't parse " + this.defaultValue, e);
                this.parsedDefaultValue = Value.getNone();
            }
            assert this.parsedDefaultValue != null;
        }
        return this.parsedDefaultValue;
    }

    /**
     * Whether this field accepts NULL.
     * 
     * @return <code>true</code> if it does, <code>false</code> if not, <code>null</code> if
     *         unknown.
     */
    public synchronized final Boolean isNullable() {
        return this.nullable;
    }

    public boolean isKey() {
        return this.isPrimaryKey() || this.isForeignKey();
    }

    /**
     * Is this the one and only field in the primary key of its table.
     * 
     * @return <code>true</code> if this is part of the primary key, and the primary key has no
     *         other fields.
     */
    public boolean isPrimaryKey() {
        return this.getTable().getPrimaryKeys().equals(Collections.singleton(this));
    }

    /**
     * Is this the one and only field in a foreign key of its table.
     * 
     * @return <code>true</code> if this is part of a foreign key that has no other fields.
     * @see #getFieldGroup()
     */
    public boolean isForeignKey() {
        final FieldGroup fieldGroup = getFieldGroup();
        return fieldGroup.getKeyType() == Type.FOREIGN_KEY && fieldGroup.getSingleField() != null;
    }

    /**
     * To which group this field belong.
     * 
     * @return the group of this field.
     * @see SQLTable#getFieldGroups()
     */
    public FieldGroup getFieldGroup() {
        return this.getTable().getFieldGroups().get(this.getName());
    }

    public final SQLTable getForeignTable() {
        return this.getDBSystemRoot().getGraph().getForeignTable(this);
    }

    public final Link getLink() {
        return this.getDBSystemRoot().getGraph().getForeignLink(this);
    }

    // *** FieldRef

    public SQLField getField() {
        return this;
    }

    public String getFieldRef() {
        return SQLBase.quoteIdentifier(this.getAlias()) + "." + SQLBase.quoteIdentifier(this.getField().getName());
    }

    public String getAlias() {
        return this.getTable().getName();
    }

    @Override
    public TableRef getTableRef() {
        return this.getTable();
    }

    /**
     * Return this field in the passed table.
     * 
     * @param table a table, e.g OBSERVATION obs.
     * @return a field in the passed table, e.g. if this is OBSERVATION.DESIGNATION then
     *         obs.DESIGNATION.
     * @throws IllegalArgumentException if this field is not in the same table as the argument.
     * @see {@link TableRef#getField(String)}
     */
    public final FieldRef getFieldRef(TableRef table) throws IllegalArgumentException {
        if (table.getTable() != this.getTable())
            throw new IllegalArgumentException("Table mismatch for " + table + " and " + this);
        return table.getField(this.getName());
    }

    public synchronized String toXML() {
        if (this.xml == null) {
            final StringBuilder sb = new StringBuilder(2048);
            sb.append("<field name=\"");
            sb.append(JDOM2Utils.OUTPUTTER.escapeAttributeEntities(this.getName()));
            sb.append("\" >");
            sb.append(this.type.toXML());
            sb.append(XMLCodecUtils.encodeSimple(this.metadata));
            sb.append("<infoSchema>");
            sb.append(XMLCodecUtils.encodeSimple(this.infoSchemaCols));
            sb.append("</infoSchema></field>\n");
            this.xml = sb.toString();
        }
        return this.xml;
    }

    @Override
    public Map<String, ? extends DBStructureItemJDBC> getChildrenMap() {
        return Collections.emptyMap();
    }

    // MAYBE equalsDesc in DBStructureItem
    public boolean equalsDesc(SQLField o) {
        return this.equalsDesc(o, null, true) == null;
    }

    // compareDefault useful when fields' default are functions containing the name of the table (eg
    // serial)
    public String equalsDesc(SQLField o, SQLSystem otherSystem, boolean compareDefault) {
        final Map<Properties, String> res = getDiffMap(o, otherSystem, compareDefault);
        if (res.size() == 0)
            return null;
        else
            return this.getSQLName() + " != " + o.getSQLName() + ":\n" + CollectionUtils.join(res.values(), "\n");
    }

    /**
     * Return the differences between this and <code>o</code>.
     * 
     * @param o another field.
     * @param otherSystem the system <code>o</code> originated from, can be <code>null</code>.
     * @param compareDefault <code>true</code> if defaults should be compared.
     * @return a map containing properties that differs and their values.
     */
    public synchronized Map<Properties, String> getDiffMap(SQLField o, SQLSystem otherSystem, boolean compareDefault) {
        if (o == null)
            return Collections.singletonMap(null, "other field is null");
        final Map<Properties, String> res = new HashMap<Properties, String>();
        if (!this.getName().equals(o.getName()))
            res.put(Properties.NAME, "name unequal : " + quoteIdentifier(this.getName()) + " != " + quoteIdentifier(o.getName()));
        if (!this.getType().equals(o.getType(), otherSystem))
            res.put(Properties.TYPE, "type unequal : " + this.getType() + " " + o.getType());
        if (!CompareUtils.equals(this.isNullable(), o.isNullable()))
            res.put(Properties.NULLABLE, "is_nullable unequal : " + this.isNullable() + " " + o.isNullable());
        if (compareDefault && !defaultEquals(o))
            res.put(Properties.DEFAULT, "default unequal : " + print(this.getDefaultValue()) + " != " + print(o.getDefaultValue()));
        return res;
    }

    private boolean defaultEquals(SQLField o) {
        final SQLSyntax syntax = this.getDBSystemRoot().getSyntax();
        final SQLSyntax oSyntax = o.getDBSystemRoot().getSyntax();
        // don't compare actual default for auto fields, e.g. on MySQL it's null on PG it
        // nextval(seq)
        if (syntax.isAuto(this) && oSyntax.isAuto(o))
            return true;
        // normalize to this syntax before doing a string comparison
        // perhaps: if that comparison fails, execute "SELECT default" and compare the java objects.
        return CompareUtils.equals(normalizeDefault(this, syntax), normalizeDefault(o, syntax));
    }

    private static String normalizeDefault(SQLField f, final SQLSyntax syntax) {
        final String def = syntax.getDefault(f);
        // no explicit default and DEFAULT NULL is equivalent
        return def != null && def.trim().toUpperCase().equals("NULL") ? null : def;
    }

    // disambiguate NULL
    private static String print(Object o) {
        return o == null ? "NULL" : "<" + o + ">";
    }

    // IFieldPath

    @Override
    public FieldPath getFieldPath() {
        return new FieldPath(this);
    }

    @Override
    public Path getPath() {
        return Path.get(getTable());
    }

    @Override
    public String getFieldName() {
        return this.getName();
    }
}
