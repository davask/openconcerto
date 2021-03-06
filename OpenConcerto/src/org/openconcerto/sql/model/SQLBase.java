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
 * DataBase created on 4 mai 2004
 */
package org.openconcerto.sql.model;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.LoadingListener.LoadingEvent;
import org.openconcerto.sql.model.LoadingListener.StructureLoadingEvent;
import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.Tuple3;
import org.openconcerto.utils.cc.CopyOnWriteMap;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.change.CollectionChangeEventCreator;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.ResultSetHandler;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * Une base de donnée SQL. Une base est unique, pour obtenir une instance il faut passer par
 * SQLServer. Une base permet d'accéder aux tables qui la composent, ainsi qu'à son graphe.
 * 
 * @author ILM Informatique 4 mai 2004
 * @see org.openconcerto.sql.model.SQLServer#getOrCreateBase(String)
 * @see #getTable(String)
 * @see #getGraph()
 */
@ThreadSafe
public final class SQLBase extends SQLIdentifier {

    /**
     * Boolean system property, if <code>true</code> then the structure and the graph of SQL base
     * will default to be loaded from XML instead of JDBC.
     * 
     * @see DBSystemRoot#useCache()
     */
    public static final String STRUCTURE_USE_XML = "org.openconcerto.sql.structure.useXML";
    /**
     * Boolean system property, if <code>true</code> then when the structure of SQL base cannot be
     * loaded from XML, the files are not deleted.
     */
    public static final String STRUCTURE_KEEP_INVALID_XML = "org.openconcerto.sql.structure.keepInvalidXML";
    /**
     * Boolean system property, if <code>true</code> then schemas and tables can be dropped,
     * otherwise the refresh will throw an exception.
     */
    public static final String ALLOW_OBJECT_REMOVAL = "org.openconcerto.sql.identifier.allowRemoval";

    static public final void logCacheError(final DBItemFileCache dir, Exception e) {
        final Logger logger = Log.get();
        if (logger.isLoggable(Level.CONFIG))
            logger.log(Level.CONFIG, "invalid files in " + dir, e);
        else
            logger.info("invalid files in " + dir + "\n" + e.getMessage());
    }

    // null is a valid name (MySQL doesn't support schemas)
    private final CopyOnWriteMap<String, SQLSchema> schemas;
    @GuardedBy("this")
    private int[] dbVersion;

    /**
     * Crée une base dans <i>server </i> nommée <i>name </i>.
     * <p>
     * Note: ne pas utiliser ce constructeur, utiliser {@link SQLServer#getOrCreateBase(String)}
     * </p>
     * 
     * @param server son serveur.
     * @param name son nom.
     * @param login the login.
     * @param pass the password.
     */
    SQLBase(SQLServer server, String name, String login, String pass) {
        this(server, name, null, login, pass, null);
    }

    /**
     * Creates a base in <i>server</i> named <i>name</i>.
     * <p>
     * Note: don't use this constructor, use {@link SQLServer#getOrCreateBase(String)}
     * </p>
     * 
     * @param server its server.
     * @param name its name.
     * @param systemRootInit to initialize the {@link DBSystemRoot} before setting the datasource.
     * @param login the login.
     * @param pass the password.
     * @param dsInit to initialize the datasource before any request (eg setting jdbc properties),
     *        can be <code>null</code>.
     */
    SQLBase(SQLServer server, String name, IClosure<? super DBSystemRoot> systemRootInit, String login, String pass, IClosure<? super SQLDataSource> dsInit) {
        super(server, name);
        if (name == null)
            throw new NullPointerException("null base");
        this.schemas = new CopyOnWriteMap<String, SQLSchema>();
        this.dbVersion = null;

        // if this is the systemRoot we must init the datasource to be able to loadTables()
        final DBSystemRoot sysRoot = this.getDBSystemRoot();
        if (sysRoot.getJDBC() == this)
            sysRoot.setDS(systemRootInit, login, pass, dsInit);
    }

    final TablesMap init(final boolean readCache) {
        try {
            return refresh(null, readCache, true);
        } catch (SQLException e) {
            throw new IllegalStateException("could not init " + this, e);
        }
    }

    @Override
    protected synchronized void onDrop() {
        // allow schemas (and their descendants) to be gc'd even we aren't
        this.schemas.clear();
        super.onDrop();
    }

    TablesMap refresh(final TablesMap namesToRefresh, final boolean readCache) throws SQLException {
        return this.refresh(namesToRefresh, readCache, false);
    }

    // what tables were loaded by JDBC
    private TablesMap refresh(final TablesMap namesToRefresh, final boolean readCache, final boolean inCtor) throws SQLException {
        if (readCache)
            return loadTables(namesToRefresh, inCtor);
        else
            return fetchTables(namesToRefresh);
    }

    private final TablesMap loadTables(TablesMap childrenNames, boolean inCtor) throws SQLException {
        this.checkDropped();
        if (childrenNames != null && childrenNames.size() == 0)
            return childrenNames;
        childrenNames = assureAllTables(childrenNames);
        final DBItemFileCache dir = getFileCache();
        synchronized (getTreeMutex()) {
            XMLStructureSource xmlStructSrc = null;
            if (dir != null) {
                try {
                    Log.get().config("for mapping " + this + " trying xmls in " + dir);
                    final long t1 = System.currentTimeMillis();
                    // don't call refreshTables() with XML :
                    // say you have one schema "s" and its file is missing or corrupted
                    // refreshTables(XML) will drop it from our children
                    // then we will call refreshTables(JDBC) and it will be re-added
                    // => so we removed our child for nothing (firing unneeded events, rendering
                    // java objects useless and possibly destroying the systemRoot path)
                    xmlStructSrc = new XMLStructureSource(this, childrenNames, dir);
                    assert xmlStructSrc.isPreVerify();
                    xmlStructSrc.init();
                    final long t2 = System.currentTimeMillis();
                    Log.get().config("XML took " + (t2 - t1) + "ms for mapping " + this.getName() + "." + xmlStructSrc.getSchemas());
                } catch (Exception e) {
                    logCacheError(dir, e);
                    // since isPreVerify() is true, schemas weren't changed.
                    // if an error reached us, we cannot trust the loaded structure (e.g.
                    // IOExceptions are handled by XMLStructureSource)
                    xmlStructSrc = null;
                }
            }

            final long t1 = System.currentTimeMillis();
            // always do the fetchTables() since XML do nothing anymore
            final JDBCStructureSource jdbcStructSrc = this.fetchTablesP(childrenNames, xmlStructSrc);
            final long t2 = System.currentTimeMillis();
            Log.get().config("JDBC took " + (t2 - t1) + "ms for mapping " + this.getName() + "." + jdbcStructSrc.getSchemas());
            return jdbcStructSrc.getTablesMap();
        }
    }

    private final TablesMap assureAllTables(final TablesMap childrenNames) {
        // don't allow partial schemas (we do the same in SQLServer.refresh()) since
        // JDBCStructureSource needs to check for SQLSchema.METADATA_TABLENAME
        final TablesMap res;
        if (childrenNames == null) {
            res = childrenNames;
        } else {
            res = TablesMap.create(childrenNames);
            for (final Entry<String, Set<String>> e : childrenNames.entrySet()) {
                final String schemaName = e.getKey();
                if (e.getValue() != null && !this.contains(schemaName)) {
                    res.put(schemaName, null);
                }
            }
        }
        return res;
    }

    /**
     * Load the structure from JDBC.
     * 
     * @param childrenNames which children to refresh, <code>null</code> meaning all.
     * @return tables actually loaded, never <code>null</code>.
     * @throws SQLException if an error occurs.
     * @see DBSystemRoot#refetch(Set)
     */
    TablesMap fetchTables(TablesMap childrenNames) throws SQLException {
        if (childrenNames != null && childrenNames.size() == 0)
            return childrenNames;
        return this.fetchTablesP(assureAllTables(childrenNames), null).getTablesMap();
    }

    private JDBCStructureSource fetchTablesP(TablesMap childrenNames, StructureSource<?> external) throws SQLException {
        // TODO pass TablesByRoot to event
        final LoadingEvent evt = new StructureLoadingEvent(this, childrenNames == null ? null : childrenNames.keySet());
        final DBSystemRoot sysRoot = this.getDBSystemRoot();
        try {
            sysRoot.fireLoading(evt);
            return this.refreshTables(new JDBCStructureSource(this, childrenNames, external == null ? null : external.getNewStructure(), external == null ? null : external.getOutOfDateSchemas()));
        } finally {
            sysRoot.fireLoading(evt.createFinishingEvent());
        }
    }

    final TablesMap loadTables() throws SQLException {
        return this.loadTables(null);
    }

    /**
     * Tries to load the structure from XMLs, if that fails fallback to JDBC.
     * 
     * @param childrenNames which children to refresh.
     * @return tables loaded with JDBC.
     * @throws SQLException if an error occurs in JDBC.
     */
    final TablesMap loadTables(TablesMap childrenNames) throws SQLException {
        return this.loadTables(childrenNames, false);
    }

    private final <T extends Exception, S extends StructureSource<T>> S refreshTables(final S src) throws T {
        this.checkDropped();
        synchronized (getTreeMutex()) {
            src.init();

            // refresh schemas
            final Set<String> newSchemas = src.getTotalSchemas();
            final Set<String> currentSchemas = src.getExistingSchemasToRefresh();
            mustContain(this, newSchemas, currentSchemas, "schemas");
            final CollectionChangeEventCreator c = this.createChildrenCreator();
            // remove all schemas that are not there anymore
            for (final String schema : CollectionUtils.substract(currentSchemas, newSchemas)) {
                this.schemas.remove(schema).dropped();
            }
            // delete the saved schemas that we could have fetched, but haven't
            // (schemas that are not in scope are simply ignored, NOT deleted)
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    for (final DBItemFileCache savedSchema : getSavedCaches(false)) {
                        if (src.isInTotalScope(savedSchema.getName()) && !newSchemas.contains(savedSchema.getName())) {
                            savedSchema.delete();
                        }
                    }
                    return null;
                }
            });

            // clearNonPersistent (will be recreated by fillTables())
            for (final String schema : CollectionUtils.inter(currentSchemas, newSchemas)) {
                this.getSchema(schema).clearNonPersistent();
            }
            // create the new ones
            for (final String schema : newSchemas) {
                this.createAndGetSchema(schema);
            }

            // refresh tables
            final Set<SQLName> newTableNames = src.getTotalTablesNames();
            final Set<SQLName> currentTables = src.getExistingTablesToRefresh();
            // we can only add, cause instances of SQLTable are everywhere
            mustContain(this, newTableNames, currentTables, "tables");
            // remove dropped tables
            for (final SQLName tableName : CollectionUtils.substract(currentTables, newTableNames)) {
                final SQLSchema s = this.getSchema(tableName.getItemLenient(-2));
                s.rmTable(tableName.getName());
            }
            // clearNonPersistent
            for (final SQLName tableName : CollectionUtils.inter(newTableNames, currentTables)) {
                final SQLSchema s = this.getSchema(tableName.getItemLenient(-2));
                s.getTable(tableName.getName()).clearNonPersistent();
            }
            // create new table descendants (including empty tables)
            for (final SQLName tableName : CollectionUtils.substract(newTableNames, currentTables)) {
                final SQLSchema s = this.getSchema(tableName.getItemLenient(-2));
                s.addTable(tableName.getName());
            }

            // fill with columns
            src.fillTables();

            this.fireChildrenChanged(c);
            // don't signal our systemRoot if our server doesn't yet reference us,
            // otherwise the server will create another instance and enter an infinite loop
            assert this.getServer().getBase(this.getName()) == this;
            final TablesMap byRoot;
            final TablesMap toRefresh = src.getToRefresh();
            if (toRefresh == null) {
                byRoot = TablesMap.createByRootFromChildren(this, null);
            } else {
                final DBRoot root = this.getDBRoot();
                if (root != null) {
                    byRoot = TablesMap.createFromTables(root.getName(), toRefresh.get(null));
                } else {
                    byRoot = toRefresh;
                }
            }
            this.getDBSystemRoot().descendantsChanged(byRoot, src.hasExternalStruct());
        }
        src.save();
        return src;
    }

    static <T> void mustContain(final DBStructureItemJDBC c, final Set<T> newC, final Set<T> oldC, final String name) {
        if (Boolean.getBoolean(ALLOW_OBJECT_REMOVAL))
            return;

        final Set<T> diff = CollectionUtils.contains(newC, oldC);
        if (diff != null)
            throw new IllegalStateException("some " + name + " were removed in " + c + ": " + diff);
    }

    public final String getURL() {
        return this.getServer().getURL(this.getName());
    }

    /**
     * Return the field named <i>fieldName </i> in this base.
     * 
     * @param fieldName the fully qualified name of the field.
     * @return the matching field or null if none exists.
     * @deprecated use {@link SQLTable#getField(String)} and {@link DBRoot#getTable(String)} or at
     *             worst {@link #getTable(SQLName)}
     */
    public SQLField getField(String fieldName) {
        String[] parts = fieldName.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException(fieldName + " is not a fully qualified name (like TABLE.FIELD_NAME).");
        }
        String table = parts[0];
        String field = parts[1];
        if (!this.containsTable(table))
            return null;
        else
            return this.getTable(table).getField(field);
    }

    /**
     * Return the table named <i>tablename </i> in this base.
     * 
     * @param tablename the name of the table.
     * @return the matching table or null if none exists.
     */
    public SQLTable getTable(String tablename) {
        return this.getTable(SQLName.parse(tablename));
    }

    public SQLTable getTable(SQLName n) {
        if (n.getItemCount() == 0 || n.getItemCount() > 2)
            throw new IllegalArgumentException("'" + n + "' is not a dotted tablename");

        if (n.getItemCount() == 1) {
            return this.findTable(n.getName());
        } else {
            final SQLSchema s = this.getSchema(n.getFirst());
            if (s == null)
                return null;
            else
                return s.getTable(n.getName());
        }
    }

    private SQLTable findTable(String name) {
        final DBRoot guessed = this.guessDBRoot();
        return guessed == null ? this.getDBSystemRoot().findTable(name) : guessed.findTable(name);
    }

    /**
     * Return whether this base contains the table.
     * 
     * @param tableName the name of the table.
     * @return true if the tableName exists.
     */
    public boolean containsTable(String tableName) {
        return contains(SQLName.parse(tableName));
    }

    private boolean contains(final SQLName n) {
        return this.getTable(n) != null;
    }

    /**
     * Return the tables in the default schema.
     * 
     * @return an unmodifiable Set of the tables' names.
     */
    public Set<String> getTableNames() {
        return this.getDefaultSchema().getTableNames();
    }

    /**
     * Return the tables in the default schema.
     * 
     * @return a Set of SQLTable.
     */
    public Set<SQLTable> getTables() {
        return this.getDefaultSchema().getTables();
    }

    // *** all*

    public Set<SQLName> getAllTableNames() {
        final Set<SQLName> res = new HashSet<SQLName>();
        for (final SQLTable t : this.getAllTables()) {
            res.add(t.getSQLName(this, false));
        }
        return res;
    }

    public Set<SQLTable> getAllTables() {
        final Set<SQLTable> res = new HashSet<SQLTable>();
        for (final SQLSchema s : this.getSchemas()) {
            res.addAll(s.getTables());
        }
        return res;
    }

    // *** schemas

    @Override
    public Map<String, SQLSchema> getChildrenMap() {
        return this.schemas.getImmutable();
    }

    public final Set<SQLSchema> getSchemas() {
        return new HashSet<SQLSchema>(this.schemas.values());
    }

    public final SQLSchema getSchema(String name) {
        return this.schemas.get(name);
    }

    /**
     * The current default schema.
     * 
     * @return the default schema or <code>null</code>.
     */
    final SQLSchema getDefaultSchema() {
        final Map<String, SQLSchema> children = this.getChildrenMap();
        if (children.size() == 0) {
            return null;
        } else if (children.size() == 1) {
            return children.values().iterator().next();
        } else if (this.getServer().getSQLSystem().getLevel(DBRoot.class) == HierarchyLevel.SQLSCHEMA) {
            final List<String> path = this.getDBSystemRoot().getRootPath();
            if (path.size() > 0)
                return children.get(path.get(0));
        }
        throw new IllegalStateException();
    }

    private SQLSchema createAndGetSchema(String name) {
        SQLSchema res = this.getSchema(name);
        if (res == null) {
            res = new SQLSchema(this, name);
            this.schemas.put(name, res);
        }
        return res;
    }

    public final DBRoot guessDBRoot() {
        if (this.getDBRoot() != null)
            return this.getDBRoot();
        else
            return this.getDBSystemRoot().getDefaultRoot();
    }

    public DatabaseGraph getGraph() {
        if (this.getDBRoot() == null)
            return this.getDBSystemRoot().getGraph();
        else
            return this.getDBRoot().getGraph();
    }

    /**
     * Vérifie l'intégrité de la base. C'est à dire que les clefs étrangères pointent sur des lignes
     * existantes. Cette méthode renvoie une Map dont les clefs sont les tables présentant des
     * inconsistences. Les valeurs de cette Map sont des List de SQLRow.
     * 
     * @return les inconsistences.
     * @see SQLTable#checkIntegrity()
     */
    public Map<SQLTable, List<Tuple3<SQLRow, SQLField, SQLRow>>> checkIntegrity() {
        final Map<SQLTable, List<Tuple3<SQLRow, SQLField, SQLRow>>> inconsistencies = new HashMap<SQLTable, List<Tuple3<SQLRow, SQLField, SQLRow>>>();
        for (final SQLTable table : this.getAllTables()) {
            List<Tuple3<SQLRow, SQLField, SQLRow>> tableInc = table.checkIntegrity();
            if (tableInc.size() > 0)
                inconsistencies.put(table, tableInc);
        }
        return inconsistencies;
    }

    /**
     * Exécute la requête dans le contexte de cette base et retourne le résultat. Le résultat d'une
     * insertion étant les clefs auto-générées, eg le nouvel ID.
     * 
     * @deprecated use getDataSource()
     * @param query le requête à exécuter.
     * @return le résultat de la requête.
     * @see java.sql.Statement#getGeneratedKeys()
     */
    public ResultSet execute(String query) {
        return this.getDataSource().executeRaw(query);
    }

    public SQLDataSource getDataSource() {
        return this.getDBSystemRoot().getDataSource();
    }

    public String toString() {
        return this.getName();
    }

    // ** metadata

    /**
     * Get a metadata.
     * 
     * @param schema the name of the schema.
     * @param name the name of the meta data.
     * @return the requested meta data, can be <code>null</code> (including if
     *         {@value SQLSchema#METADATA_TABLENAME} does not exist).
     */
    String getFwkMetadata(String schema, String name) {
        return getFwkMetadata(Collections.singletonList(schema), name).get(schema);
    }

    private final String getSel(final String schema, final String name, final boolean selSchema) {
        final SQLName tableName = new SQLName(this.getName(), schema, SQLSchema.METADATA_TABLENAME);
        return "SELECT " + (selSchema ? this.quoteString(schema) + ", " : "") + "\"VALUE\" FROM " + tableName.quote() + " WHERE \"NAME\"= " + this.quoteString(name);
    }

    private final void exec(final Collection<String> schemas, final String name, final ResultSetHandler rsh) {
        this.getDataSource().execute(CollectionUtils.join(schemas, "\nUNION ALL ", new ITransformer<String, String>() {
            @Override
            public String transformChecked(String schema) {
                // schema name needed since missing values will result in missing rows not
                // null values
                return getSel(schema, name, true);
            }
        }), new IResultSetHandler(rsh, false));
    }

    Map<String, String> getFwkMetadata(final Collection<String> schemas, final String name) {
        if (schemas.isEmpty())
            return Collections.emptyMap();
        final Map<String, String> res = new LinkedHashMap<String, String>();
        CollectionUtils.fillMap(res, schemas);
        final ResultSetHandler rsh = new ResultSetHandler() {
            @Override
            public Object handle(ResultSet rs) throws SQLException {
                while (rs.next()) {
                    res.put(rs.getString(1), rs.getString(2));
                }
                return null;
            }
        };
        try {
            if (this.getDataSource().getTransactionPoint() == null) {
                exec(schemas, name, rsh);
            } else {
                // If already in a transaction, don't risk aborting it if a table doesn't exist.
                // (it's not strictly required for H2 and MySQL, since the transaction is *not*
                // aborted)
                SQLUtils.executeAtomic(this.getDataSource(), new ConnectionHandlerNoSetup<Object, SQLException>() {
                    @Override
                    public Object handle(SQLDataSource ds) throws SQLException {
                        exec(schemas, name, rsh);
                        return null;
                    }
                }, false);
            }
        } catch (Exception exn) {
            final SQLException sqlExn = SQLUtils.findWithSQLState(exn);
            final boolean tableNotFound = sqlExn != null && (sqlExn.getSQLState().equals("42S02") || sqlExn.getSQLState().equals("42P01"));
            if (!tableNotFound)
                throw new IllegalStateException("Not a missing table exception", exn);

            // The following fall back should not currently be needed since the table is created
            // by JDBCStructureSource.getNames(). Even without that most DB should contain the
            // metadata tables.

            // if only one schema, there's no ambiguity : just return null value
            // otherwise retry with each single schema to find out which ones are missing
            if (schemas.size() > 1) {
                // this won't loop indefinetly since schemas.size() will be 1
                for (final String schema : schemas)
                    res.put(schema, this.getFwkMetadata(schema, name));
            }
        }
        return res;
    }

    public final String getMDName() {
        return this.getServer().getSQLSystem().getMDName(this.getName());
    }

    public synchronized int[] getVersion() throws SQLException {
        if (this.dbVersion == null) {
            this.dbVersion = this.getDataSource().useConnection(new ConnectionHandlerNoSetup<int[], SQLException>() {
                @Override
                public int[] handle(SQLDataSource ds) throws SQLException, SQLException {
                    final DatabaseMetaData md = ds.getConnection().getMetaData();
                    return new int[] { md.getDatabaseMajorVersion(), md.getDatabaseMinorVersion() };
                }
            });
        }
        return this.dbVersion;
    }

    // ** files

    static final String FILENAME = "structure.xml";

    static final boolean isSaved(final SQLServer s, final String base, final String schema) {
        return s.getFileCache().getChild(base, schema).getFile(SQLBase.FILENAME).exists();
    }

    /**
     * Where xml dumps are saved, always <code>null</code> if {@link DBSystemRoot#useCache()} is
     * <code>false</code>.
     * 
     * @return the directory of xmls dumps, <code>null</code> if it can't be found.
     */
    private final DBItemFileCache getFileCache() {
        final boolean useXML = this.getDBSystemRoot().useCache();
        final DBFileCache fileCache = this.getServer().getFileCache();
        if (!useXML || fileCache == null)
            return null;
        else {
            return fileCache.getChild(this.getName());
        }
    }

    private final DBItemFileCache getSchemaFileCache(String schema) {
        final DBItemFileCache item = this.getFileCache();
        if (item == null)
            return null;
        return item.getChild(schema);
    }

    final List<DBItemFileCache> getSavedShemaCaches() {
        return this.getSavedCaches(true);
    }

    private final List<DBItemFileCache> getSavedCaches(boolean withStruct) {
        final DBItemFileCache item = this.getFileCache();
        if (item == null)
            return Collections.emptyList();
        else {
            return item.getSavedDesc(SQLSchema.class, withStruct ? FILENAME : null);
        }
    }

    final boolean isSaved(String schema) {
        return isSaved(this.getServer(), this.getName(), schema);
    }

    /**
     * Deletes all files containing information about this base's structure.
     */
    public void deleteStructureFiles() {
        for (final DBItemFileCache f : this.getSavedCaches(true)) {
            f.getFile(FILENAME).delete();
        }
    }

    boolean save(final String schemaName) {
        final DBItemFileCache schemaFileCache = this.getSchemaFileCache(schemaName);
        if (schemaFileCache == null) {
            return false;
        } else {
            final File schemaFile = schemaFileCache.getFile(FILENAME);
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    Writer pWriter = null;
                    try {
                        final String schema = getSchema(schemaName).toXML();
                        if (schema == null)
                            return false;
                        FileUtils.mkdir_p(schemaFile.getParentFile());
                        // Might save garbage if two threads open the same file
                        synchronized (this) {
                            pWriter = FileUtils.createXMLWriter(schemaFile);
                            pWriter.write("<root codecVersion=\"" + XMLStructureSource.version + "\" >\n" + schema + "\n</root>\n");
                        }

                        return true;
                    } catch (Exception e) {
                        Log.get().log(Level.WARNING, "unable to save files in " + schemaFile, e);
                        return false;
                    } finally {
                        if (pWriter != null) {
                            try {
                                pWriter.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        }
    }

    // *** quoting

    // * quote

    /**
     * Quote %-escaped parameters. %% : %, %s : {@link #quoteString(String)}, %i : an identifier
     * string, if it's a SQLName calls {@link SQLName#quote()} else {@link #quoteIdentifier(String)}
     * , %f or %n : respectively fullName and name of an SQLIdentifier of a DBStructureItem.
     * 
     * @param pattern a string with %, eg "SELECT * FROM %n where %f like '%%a%%'".
     * @param params the parameters, eg [ /TENSION/, |TENSION.LABEL| ].
     * @return pattern with % replaced, eg SELECT * FROM "TENSION" where "TENSION.LABEL" like '%a%'.
     */
    public final String quote(final String pattern, Object... params) {
        return quote(this, pattern, params);
    }

    // since Strings might not be quoted correctly
    @Deprecated
    public final static String quoteStd(final String pattern, Object... params) {
        return quote(null, pattern, params);
    }

    static private final Pattern percent = Pattern.compile("%.");

    private final static String quote(final SQLBase b, final String pattern, Object... params) {
        final SQLSyntax s = b == null ? null : SQLSyntax.get(b);
        final Matcher m = percent.matcher(pattern);
        final StringBuffer sb = new StringBuffer();
        int i = 0;
        int lastAppendPosition = 0;
        while (m.find()) {
            final String replacement;
            final char modifier = m.group().charAt(m.group().length() - 1);
            if (modifier == '%') {
                replacement = "%";
            } else {
                final Object param = params[i++];
                if (modifier == 's') {
                    replacement = SQLSyntax.quoteString(s, param.toString());
                } else if (modifier == 'i') {
                    if (param instanceof SQLName)
                        replacement = ((SQLName) param).quote();
                    else
                        replacement = quoteIdentifier(param.toString());
                } else {
                    final SQLIdentifier ident = (SQLIdentifier) ((DBStructureItem<?>) param).getJDBC();
                    if (modifier == 'f') {
                        replacement = ident.getSQLName().quote();
                    } else if (modifier == 'n')
                        replacement = quoteIdentifier(ident.getName());
                    else
                        throw new IllegalArgumentException("unknown modifier: " + modifier);
                }
            }

            // do NOT use appendReplacement() (and appendTail()) since it parses \ and $
            // Append the intervening text
            sb.append(pattern.subSequence(lastAppendPosition, m.start()));
            // Append the match substitution
            sb.append(replacement);
            lastAppendPosition = m.end();
        }
        sb.append(pattern.substring(lastAppendPosition));
        return sb.toString();
    }

    // * quoteString

    /**
     * Quote an sql string specifically for this base.
     * 
     * @param s an arbitrary string, eg "salut\ l'ami".
     * @return the quoted form, eg "'salut\\ l''ami'".
     * @see #quoteStringStd(String)
     */
    public String quoteString(String s) {
        return SQLSyntax.get(this).quoteString(s);
    }

    static private final Pattern singleQuote = Pattern.compile("'", Pattern.LITERAL);
    static public final Pattern quotedPatrn = Pattern.compile("'(('')|[^'])*'");
    static private final Pattern twoSingleQuote = Pattern.compile("''", Pattern.LITERAL);

    /**
     * Quote an sql string the standard way. See section 4.1.2.1. String Constants of postgresql
     * documentation.
     * 
     * @param s an arbitrary string, eg "salut\ l'ami".
     * @return the quoted form, eg "'salut\ l''ami'".
     */
    public final static String quoteStringStd(String s) {
        return s == null ? "NULL" : "'" + singleQuote.matcher(s).replaceAll("''") + "'";
    }

    /**
     * Unquote an SQL string the standard way.
     * <p>
     * NOTE : There's no unquoteString() instance method since it can be affected by session
     * parameters. So to be correct the method should execute a request each time to find out these
     * values. But if it did that, it might as well execute <code>"SELECT ?"</code> with the string
     * (and <b>not</b> <code>executeScalar("SELECT " + s)</code> to avoid SQL injection).
     * </p>
     * 
     * @param s an arbitrary SQL string, e.g. 'salu\t l''ami'.
     * @return the java string, e.g. "salu\\t l'ami".
     * @see #quoteStringStd(String)
     */
    public final static String unquoteStringStd(String s) {
        if (!quotedPatrn.matcher(s).matches())
            throw new IllegalArgumentException("Invalid quoted string " + s);
        return twoSingleQuote.matcher(s.substring(1, s.length() - 1)).replaceAll("'");
    }

    // * quoteIdentifier

    static private final Pattern doubleQuote = Pattern.compile("\"");

    /**
     * Quote a sql identifier to prevent it from being folded and allow any character.
     * 
     * @param identifier a SQL identifier, eg 'My"Table'.
     * @return the quoted form, eg '"My""Table"'.
     */
    public static final String quoteIdentifier(String identifier) {
        return '"' + doubleQuote.matcher(identifier).replaceAll("\"\"") + '"';
    }
}
