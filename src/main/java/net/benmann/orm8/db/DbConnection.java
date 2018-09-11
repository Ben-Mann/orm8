package net.benmann.orm8.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import net.benmann.orm8.db.VersionTable.VersionRecord;

/**
 * A db connection. Pass the jdbc connection string, or use fromEnvironment() to
 * initialise with an environment setting.
 */
public abstract class DbConnection<DBT extends DbConnection<DBT>> implements AutoCloseable {
    private final List<AbstractTable<?, ?, DBT>> tables = new ArrayList<AbstractTable<?, ?, DBT>>();
    protected Connection connection;
    protected final VersionTable<DBT> _versions = new VersionTable<DBT>(getConnection()); // create(Version.<DBT> createFactory());
    protected AtomicInteger instanceReferences = new AtomicInteger(1); //There's always 1 reference on creation.
    protected String connectionString;

    //FIXME this is stupid
    //static private Map<String, DbConnection<?>> instanceMap = new HashMap<>();

    //FIXME this is also stupid. We could be connecting to a different file, which needs the migration.
    //If we've migrated a given class already, don't migrate it again.
    static private Set<Class<?>> migratedConnections = new HashSet<>();

    @SuppressWarnings("unchecked")
    private DBT getConnection() {
        return (DBT) this;
    }

    protected static interface CreateDBFn<T> {
        T create();
    }

    /**
     * See also withTransaction, which is the preferred API
     * Use as
     * 
     * <pre>
     * try (Transaction t = db.createTransaction()) {
     *     db.doSomething();
     *     db.doSomethingElse();
     *     t.commit();
     * } catch (SillinessError e) {
     *     t.rollback();
     * }
     * </pre>
     */
    public Transaction createTransaction() {
        return new Transaction(this);
    }

    /**
     * Use as
     * 
     * <pre>
     * db.withTransaction(() -> {
     *     try {
     *         db.doSomething();
     *         db.doSomethingElse();
     *     } catch (SillinessError e) {
     *         return Transaction.ROLLBACK;
     *     }
     *     return Transaction.COMMIT;
     * });
     * </pre>
     */
    public void withTransaction(Supplier<Transaction.Result> dbActions) {
        try (Transaction t = createTransaction()) {
            t.setResult(dbActions.get());
        }
    }

    public static class Result<A, B> {
        final A a;
        final B b;

        public Result(A a, B b) {
            this.a = a;
            this.b = b;
        }
    }

    public <T> Result<Transaction.Result, T> result(Transaction.Result a, T b) {
        return new Result<>(a, b);
    }

    public <T> T getWithTransaction(Supplier<Result<Transaction.Result, T>> dbActions) {
        try (Transaction t = createTransaction()) {
            Result<Transaction.Result, T> result = dbActions.get();
            t.setResult(result.a);
            return result.b;
        }
    }

    @Override public String toString() {
        return connectionString;
    }

    protected static interface RefFn<T> {
        T f();
    }

    //Miss manners says clean up!
    @Override protected void finalize() {
        if (connection != null)
            throw new ORM8RuntimeException("DbConnection not closed for " + connectionString);
    }

    /**
     * Removes the single reference to this db.
     * 
     * @throws SQLException
     */
    @Override public void close() throws SQLException {
        connection.close();
        connection = null;
    }

    public boolean isOpen() throws SQLException {
        return connection != null && !connection.isClosed();
    }


    //TODO refactor all these functional interfaces in orm8 to one f0
    public static interface CreateStatementFn {
        PreparedStatement f(Connection c) throws SQLException;
    }

    Map<Integer, PreparedStatement> cachedStatements = new HashMap<>();

    public PreparedStatement getStatement(int statementKey, CreateStatementFn fn) throws SQLException {
        PreparedStatement result = cachedStatements.get(statementKey);
        if (result != null)
            return result;

        result = fn.f(connection);
        return result;
    }

    /**
     * Called to create a new instance of a db class. Note that this API should be changed to eliminate
     * the use of statics when possible - it potentially prevents unit tests from operating completely independently.
     * FIXME the connectionString is currently db type dependent. We should be passing a generic struct to whichever
     * implementation we're going to use, and let it build the connectionstring.
     */
    static protected <T extends DbConnection<T>> T create(String connectionString, CreateDBFn<T> fn) {
        try {
            T result = fn.create();

            result.connectionString = connectionString;

            result.connection = DriverManager.getConnection(connectionString);

            synchronized (migratedConnections) {
                migratedConnections.add(result.getClass());
            }

            result.migrate();

            result.init();

            return result;
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    //    public static abstract class Factory<Q extends AbstractRecord<Q, D>, T extends AbstractTable<Q, D>, D extends DbConnection<D>> {
    //        abstract public T createTable(Q row);
    //
    //        abstract public Q createRow(D db);
    //
    //        abstract public String tableName();
    //
    //        public final CachedRecordData cachedRecordData = new CachedRecordData();
    //    }
    //
    //    public static abstract class Factory2<Q extends AbstractRecord<Q, D>, D extends DbConnection<D>> extends Factory<Q, AbstractTable<Q, D>, D> {
    //    }

    //    /** Create the specified table entry in this db instance */
    //    protected <T extends AbstractTable<R, D>, R extends AbstractRecord<R, D>, D extends DbConnection<D>> T create(Factory<R, T, D> tf) {
    //        return tf.createTable(tf.createRow((D) this));
    //    }
    //protected T create(

    //    //TODO you could track the tables here for migrations.
    //    protected AbstractTable<?, ?> onCreate(AbstractTable<?, ?> table) {
    //        return table;
    //    }

    /**
     * Set the connection parameters.
     */
    protected void init() {

    }

    /**
     * Migrates the database from its previous definition to the current one.
     * If the table doesn't already exist, we check the version table for a record
     * for this table and find nothing. So we create the table, and add a version record.
     * If the table already existed, we check how many entries there are - if they're less than
     * or equal to the number of manually created migrations for this table, we'll update the table.
     * 
     */
    protected void migrate() {
        java.util.Date dateNow = new java.util.Date();
        SQLiteBuilder builder = createBuilder();
        for (AbstractTable<?, ?, ?> table : tables) {
            if (table.migrated() < table.migrations()) {
                //FIXME this doesn't yet implement table updates.
                builder.createMigration(table).up(this);
                VersionRecord<DBT> v = _versions.create();
                v.table.set(table.helper.getTable().getRecordSource().tableName);
                v.version.set(1);
                v.updated.set(dateNow);
                v.insert();
            }
        }
    }

    protected final List<AbstractTable<?, ?, DBT>> getTables() {
        return tables;
    }

    public void addTable(AbstractTable<?, ?, DBT> table) {
        tables.add(table);
    }

    SQLiteBuilder createBuilder() {
        return new SQLiteBuilder(this);
    }

    /**
     * Manually execute an sql command.
     */
    public void exec(String sql) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new DbException(connectionString + "\nError executing " + sql, e);
        }
    }

    /** Run arbitrary SQL, without any type checking. */
    public SingleQuery query(String sql, Object... params) {
        PreparedStatement stmt = prepare(sql);

        try {
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }
        } catch (SQLException e1) {
            throw new DbException(e1);
        }

        return runQuery(stmt);
    }

    PreparedStatement prepare(String sql) {
        PreparedStatement ps;
        try {
            ps = connection.prepareStatement(sql);
        } catch (SQLException e) {
            throw new DbException(sql, e);
        }
        return ps;
    }

    SingleQuery runQuery(PreparedStatement statement) {
        try {
            if (!statement.execute()) {
                statement.close();
                return null;
            }

            return new SingleQuery(statement);
        } catch (SQLException e) {
            try {
                statement.close();
            } catch (SQLException e1) {
                throw new DbException(e1);
            }
            throw new DbException(e);
        }
    }

    public DbStatement statement(String sql) {
        return new DbStatementNoCache(sql);
    }

    public DbStatement statement(String sql, String... params) {
        return new DbStatementCached(sql, params);
    }

    public <T> T query(DbStatement statement, MapSingleResult<T> handler) {
        return statement.query(this, handler);
    }

    public <T> List<T> query(DbStatement statement, MapRow<T> handler) {
        return statement.query(this, handler);
    }


}
