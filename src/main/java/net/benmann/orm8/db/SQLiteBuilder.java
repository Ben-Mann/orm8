package net.benmann.orm8.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.benmann.orm8.db.AbstractTable.JoinedTable;
import net.benmann.orm8.db.Aggregate.Fn;
import net.benmann.orm8.db.OrderImpl.ColumnOrder;
import net.benmann.orm8.db.RecordSource.SingleSource;
import net.benmann.orm8.db.SingleTableFilter.BinarySTFilter;
import net.benmann.orm8.db.SingleTableFilter.ColumnSTFilter;
import net.benmann.orm8.db.SingleTableFilter.LogicSTFilter;

// FIXME: Split out interface
// JdbcBuilder      # Basic interface
//     |
//  SQLBuilder      # SQL Specific?
//     |
//  ---+----+
//          |
//    SQLiteBuilder # Override only the specific differences.
public class SQLiteBuilder {
    final DbConnection<?> connection;

    SQLiteBuilder(DbConnection<?> connection) {
        this.connection = connection;
    }

    public Migration createMigration(AbstractTable<?, ?, ?> table) {
        String tableName = table.getRecordSource().tableName;
        return new Migration() {
            @Override protected void up(DbConnection<?> db) {
                StringBuilder sql = new StringBuilder();
                sql.append("CREATE TABLE ").append(tableName).append(" (\n");
                //For each row
                boolean first = true;
                int autoIncrementCount = 0;
                StringBuilder keyBuilder = new StringBuilder();
                int keys = 0;
                for (Column<?> c : table.helper.fields.columns.values()) {
                    if (!first) {
                        sql.append(",\n");
                    }
                    first = false;
                    //ie
                    //id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT
                    //or
                    //id INTEGER NOT NULL
                    //PRIMARY KEY (id, foo)
                    sql.append(c.name).append(" ");
                    switch (c.columnType.dataType) {
                    case INTEGER:
                        sql.append("INTEGER");
                        break;
                    case LONG:
                        sql.append("INTEGER");
                        break;
                    case STRING:
                        sql.append("TEXT");
                        break;
                    case DOUBLE_ARRAY:
                        sql.append("BLOB");
                        break;
                    case DOUBLE:
                        sql.append("REAL");
                        break;
                    case DATE:
                        sql.append("TEXT");
                        break;
                    case BOOLEAN:
                        sql.append("INTEGER");
                        break;
                    }
                    if (!c.keyType.contains(KeyType.NULLABLE)) {
                        sql.append(" NOT NULL");
                    }
                    if (c.keyType.contains(KeyType.AUTOINCREMENT)) {
                        autoIncrementCount++;
                        sql.append(" PRIMARY KEY AUTOINCREMENT");
                    }
                    if (c.keyType.contains(KeyType.KEY)) {
                        if (keys != 0) {
                            keyBuilder.append(", ");
                        }
                        keyBuilder.append(c.name);
                        keys++;
                    }
                }
                if (keys > 0 && autoIncrementCount > 0) {
                    throw new RuntimeException("Invalid key specification in " + tableName + ". You cannot specify a KEY as well as AUTOINCREMENT.");
                } else if (autoIncrementCount > 1) {
                    throw new RuntimeException("Invalid key specification in " + tableName + ". You cannot specify multiple AUTOINCREMENT columns.");
                }
                if (keys > 0) {
                    sql.append(",\nPRIMARY KEY (").append(keyBuilder).append(")");
                }
                sql.append("\n)");
                //System.err.println("migration: exec " + sql.toString());
                db.exec(sql.toString());
            }

            @Override protected void down(DbConnection<?> db) {
                throw new RuntimeException("We cannot migrate down yet");
            }

        };
    }


    



    /** Delete only the specific record */
    <R extends AbstractSingleTableRecord<R>> void delete(R record) {
        QuerySource<R> qs = new QuerySource<>(record, QuerySource.NO_ALIAS);

        SQLBuilder sql = new SQLBuilder("DELETE").add(qs.fromClause().parts);
        Column<?>[] columns = record.getColumns();
        List<Column<?>> params = new ArrayList<Column<?>>();

        sql.add("WHERE");

        //Construct a list of column values for the primary key.
        for (Column<?> column : columns) {
            if (Collections.disjoint(column.keyType, KeyType.keys))
                continue;

            if (column.dirty)
                throw new RuntimeException("Dirty " + column.name + " in " + record.getTable().getRecordSource().tableName + " update.");

            if (!params.isEmpty())
                sql.add("AND");

            sql.add(qs.columnSQL(column)).add("=?");
            params.add(column);
        }

        try (PreparedStatement stmt = connection.connection.prepareStatement(sql.toString())) {
            int index = 1;
            for (Column<?> column : params) {
                column.set(stmt, index++);
            }
            stmt.execute();
        } catch (SQLException e) {
            throw new DbException(sql.toString(), e);
        }
    }

    void update(AbstractSingleTableRecord<?> record) {
        String tableName = record.getTable().getRecordSource().tableName;
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        Column<?>[] columns = record.getColumns();
        List<Column<?>> params = new ArrayList<Column<?>>();
        for (Column<?> column : columns) {
            if (!column.dirty)
                continue;

            if (!Collections.disjoint(column.keyType, KeyType.keys))
                throw new RuntimeException("Invalid update request for " + column.name + " in " + tableName);

            if (!params.isEmpty())
                sql.append(", ");

            sql.append(column.name).append("=?");
            params.add(column);
        }

        // WHERE - must be the primary key.
        sql.append(" WHERE ");
        int whereIndexStart = params.size();
        for (Column<?> column : columns) {
            if (Collections.disjoint(column.keyType, KeyType.keys))
                continue;

            if (column.dirty)
                throw new RuntimeException("Dirty " + column.name + " in " + tableName + " update.");

            if (params.size() != whereIndexStart)
                sql.append(" AND ");

            sql.append(column.name).append("=?");
            params.add(column);
        }

        try (PreparedStatement stmt = connection.connection.prepareStatement(sql.toString())) {
            int index = 1;
            for (Column<?> column : params) {
                column.set(stmt, index++);
            }
            stmt.execute();
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    //XXX At present we don't cache queries, though this should be done eventually.
    void insert(AbstractSingleTableRecord<?> record) {
        String tableName = record.getTable().getRecordSource().tableName;
        Column<?>[] columns = record.getColumns();

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder valuesSql = new StringBuilder(") VALUES (");
        List<Integer> insertParams = new ArrayList<>();
        List<Integer> insertKeys = new ArrayList<>();

        List<String> insertColumnIndexList = new ArrayList<>();
        int args = 0;
        for (int c = 0; c < columns.length; c++) {
            Column<?> column = columns[c];

            //Nullable columns don't get added to the expression at all.
            if (column.keyType.contains(KeyType.NULLABLE) && column.get() == null)
                continue; //don't add it at all.

            //However autoincrement keys DO get added, because we need the expression to return the value assigned to the key
            if (column.keyType.contains(KeyType.AUTOINCREMENT)) {
                insertColumnIndexList.add(column.name);
                insertKeys.add(c);
            }

            if (args != 0) {
                sql.append(", ");
                valuesSql.append(", ");
            }

            sql.append(column.name);
            if (column.keyType.contains(KeyType.AUTOINCREMENT)) {
                valuesSql.append("null");
            } else {
                valuesSql.append("?");
            }

            if (!column.keyType.contains(KeyType.AUTOINCREMENT)) {
                insertParams.add(c);
            }

            args++;
        }

        sql.append(valuesSql);
        sql.append(")");
        String insertSql = sql.toString();

        try {
            try (PreparedStatement stmt = connection.connection.prepareStatement(insertSql, insertColumnIndexList.toArray(new String[] {}))) {

                int i = 1; // IMPORTANT: 1 based.
                for (int c : insertParams) {
                    Column<?> param = columns[c];

                    //Autoincrement params have been removed from the list already.
                    if (param.get() == null && param.isNullable)
                        continue;

                    if (!param.isNullable && param.get() == null)
                        throw new DbException("The parameter " + param.name + " of " + tableName + " cannot be null.");

                    param.set(stmt, i++);
                }

                stmt.execute();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    // iterate keys
                    int index = 1;
                    for (int c : insertKeys) {
                        Column<?> column = columns[c];
                        column.update(rs, index++);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DbException(insertSql, e);
        }

    }

    <R extends ORM8Record<R>> void delete(R helper, SingleTableFilter where) {
    	if (helper == null)
    		throw new DbException("Deletion without a table is not supported.");
    	
        QuerySource<R> qs = new QuerySource<>(helper, QuerySource.NO_ALIAS);

        List<SQLFilterParam<?>> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("DELETE ").append(qs.fromClause());
        if (where != null) {
            sql.append(" WHERE ");
            SQLFilterBuilder sfb = qs.toSQL(where);
            sql.append(sfb.parts);
        	params.addAll(sfb.params);
        }

        PreparedStatement stmt = connection.prepare(sql.toString());

        int index = 0;
        for (SQLFilterParam<?> param : params) {
            param.set(stmt, ++index); //PreparedStatement params are 1-based
        }

        connection.runQuery(stmt);
    }
    
    public static class SQLBuilder {
        private final List<String> parts = new ArrayList<>();

        public SQLBuilder() {

        }

        public SQLBuilder(String... strings) {
            parts.addAll(Arrays.asList(strings));
        }

        public SQLBuilder add(SQLBuilder section) {
            parts.addAll(section.parts);
            return this;
        }

        public SQLBuilder add(String part) {
            parts.add(part);
            return this;
        }

        public SQLBuilder add(Integer value) {
            parts.add(value.toString());
            return this;
        }

        @Override public String toString() {
            return String.join(" ", parts);
        }
    }

    <R extends ORM8Record<R>> SingleQuery query(SingleTableFilter where, R helper, Integer top, SelectColumns<R> columns, OrderFn<R> order) {
    	if (helper == null)
    		throw new DbException("Count without a table is not supported.");
    	
        QuerySource<R> qs = new QuerySource<>(helper);

        List<SQLFilterParam<?>> params = new ArrayList<>();

        String columnSql = qs.toSQL(columns);
        
        SQLFilterBuilder fb = qs.fromClause();
        SQLBuilder sql = new SQLBuilder("SELECT").add(columnSql).add(fb.parts);
        params.addAll(fb.params);

        if (where != null) {
            sql.add("WHERE");
            SQLFilterBuilder sfb = qs.toSQL(where);
            sql.add(sfb.parts);
            params.addAll(sfb.params);
        }
        
        if (order != null) {
            OrderImpl orderCfg = order.f(helper);
            if (!orderCfg.columns.isEmpty()) {
                sql.add("ORDER BY");
                boolean first = true;
                for (ColumnOrder columnOrder : orderCfg.columns) {
                    if (!first) {
                        sql.add(",");
                    }

                    sql.add(qs.columnSQL(columnOrder.column));

                    switch (columnOrder.direction) {
                    case ASCENDING:
                        sql.add("ASC");
                        break;
                    case DESCENDING:
                        sql.add("DESC");
                        break;
                    }

                    first = false;
                }
            }
        }

        if (top != null) {
            sql.add("LIMIT").add(top);
        }

        try {
            System.err.println(sql.toString());
            PreparedStatement stmt = connection.prepare(sql.toString());

            int index = 0;
			try {
				for (SQLFilterParam<?> param : params) {
	                param.set(stmt, ++index); //PreparedStatement params are 1-based
	            }
			} catch (ORM8RuntimeException e) {
                throw new ORM8RuntimeException("Error preparing query for [" + qs.fromClause() + "] : " + sql, e);
			}

            SingleQuery sq = connection.runQuery(stmt);
            if (sq.rs == null)
                return null;

            return sq;
        } catch (Throwable t) {
            throw new DbException(sql.toString(), t);
        }
    }

    <R extends ORM8Record<R>> int countAll(R helper, SingleTableFilter where) {
        if (helper == null)
            throw new DbException("CountAll without a table is not supported.");

        QuerySource<R> qs = new QuerySource<>(helper);

        List<SQLFilterParam<?>> params = new ArrayList<>();
        SQLFilterBuilder fromBuilder = qs.fromClause();
        SQLBuilder sql = new SQLBuilder("SELECT COUNT(*)").add(fromBuilder.parts);
        params.addAll(fromBuilder.params);

        if (where != null) {
            sql.add("WHERE");
            SQLFilterBuilder sfb = qs.toSQL(where);
            sql.add(sfb.parts);
            params.addAll(sfb.params);
        }

        try (PreparedStatement ps = connection.connection.prepareStatement(sql.toString())) {
            int index = 0;
            for (SQLFilterParam<?> param : params) {
                param.set(ps, ++index); //PreparedStatement params are 1-based
            }

            if (!ps.execute())
                return 0;

            try (ResultSet rs = ps.getResultSet()) {
                if (rs != null)
                    return rs.getInt(1);

                return 0;
            }
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    private static class QuerySource<R extends ORM8Record<R>> {
        final Map<SingleSource<?, ?, ?>, String> sourceToAlias = new HashMap<>();
        final ORM8Table<?, ?, ?> root;
        final R helper;
        final boolean useAlias;

        static public final boolean NO_ALIAS = false;

        QuerySource(R helper) {
            this(helper, true);
        }

        QuerySource(R helper, boolean useAlias) {
            this.useAlias = useAlias;
            if (helper == null) {
                throw new DbException("Cannot query without a record.");
            }
            this.helper = helper;
            root = helper.getTable();
            if (root == null) {
                throw new DbException("Cannot query from a record with no tables.");
            }
            if (useAlias) {
                add(root);
            }
        }

        void add(ORM8Table<?, ?, ?> table) {
            RecordSource<?, ?, ?> source = table.getRecordSource();
            if (source instanceof SingleSource) {
                int idx = sourceToAlias.size();
                sourceToAlias.put((SingleSource<?, ?, ?>) source, "orm8_" + idx);
                return;
            }
            for (ORM8Table<?, ?, ?> t : table.allTables()) {
                add(t);
            }
        }

        void addTo(SQLBuilder parts, List<SQLFilterParam<?>> params, ORM8Table<?, ?, ?> table) {
            if (table instanceof AbstractTable) {
                AbstractTable t = (AbstractTable) table;
                SingleSource ss = t.getRecordSource();
                parts.add(ss.tableName);
                if (useAlias) {
                    parts.add(sourceToAlias.get(ss));
                }
                return;
            }

            JoinedTable jt = (JoinedTable) table;
            JoinedSource js = jt.getRecordSource();
            addTo(parts, params, js.left);
            parts.add("INNER JOIN"); //FIXME support different join types in the JoinedSource
            addTo(parts, params, js.right);
            if (js.condition != null) {
                parts.add("ON");
                SQLFilterBuilder builder = toSQL(js.condition.build(jt.helper));
                parts.add(builder.parts);
                params.addAll(builder.params);
            }
        }

        SQLFilterBuilder fromClause() {
            SQLBuilder parts = new SQLBuilder();
            List<SQLFilterParam<?>> params = new ArrayList<>();
            parts.add("FROM");
            addTo(parts, params, root);
            return new SQLFilterBuilder(parts, params);
        }

        private void aggregate(StringBuilder sb, Fn fn, Column<?> column) {
            switch (fn) {
            case MIN:
            case SUM:
            case AVG:
            case MAX:
                sb.append(fn.name()).append("(").append(columnSQL(column)).append(")");
                break;
            default:
                throw new DbException("Unsupported column aggregate " + fn);
            }
        }

        String toSQL(SelectColumns<R> columns) {
            if (columns == null)
                return "*";

            StringBuilder sb = new StringBuilder();
            SelectedColumns sc = columns.get(helper);
            if (sc.columns == null) {
                //FIXME this is corny as heck.
                aggregate(sb, sc.aggregate.fn, sc.aggregate.column);
            } else {
                Column<?>[] columnList = sc.columns;
                if (columnList == null || columnList.length == 0)
                    return "*";

                for (int i = 0; i < columnList.length; i++) {
                    if (i != 0) {
                        sb.append(",");
                    }
                    sb.append(columnSQL(columnList[i]));
                }
            }
            return sb.toString();
        }

        /**
         * Return a valid SQL condition based on the given Filter.
         */
        SQLFilterBuilder toSQL(SingleTableFilter where) {
            if (where == null) {
                return new SQLFilterBuilder(new SQLBuilder(), new ArrayList<>());
            }
            SQLBuilder parts = new SQLBuilder();
            List<SQLFilterParam<?>> params = new ArrayList<>();
            buildSQL(where, parts, params);
            return new SQLFilterBuilder(parts, params);
        }

        /**
         * Add to a stringbuilder based on a particular clause type.
         */
        void buildSQL(SingleTableFilter where, SQLBuilder parts, List<SQLFilterParam<?>> params) {
            if (where instanceof BinarySTFilter) {
                binaryToSQL((BinarySTFilter<?>) where, parts, params);
            } else if (where instanceof LogicSTFilter) {
                logicToSQL((LogicSTFilter) where, parts, params);
            } else if (where instanceof ColumnSTFilter) {
                columnToSQL((ColumnSTFilter<?>) where, parts, params);
            } else {
                throw new IllegalStateException("Unknown type of " + where);
            }
        }

        void logicToSQL(LogicSTFilter filter, SQLBuilder parts, List<SQLFilterParam<?>> params) {
            switch (filter.getClause()) {
            case AND:
                parts.add("(");
                buildSQL(filter.getLeft(), parts, params);
                parts.add(") AND (");
                buildSQL(filter.getRight(), parts, params);
                parts.add(")");
                break;
            default:
                throw new IllegalStateException("Unknown logic filter " + filter.getClause());
            }
        }

        void binaryToSQL(BinarySTFilter<?> filter, SQLBuilder parts, List<SQLFilterParam<?>> params) {
            switch (filter.getClause()) {
            case EQ:
                addColumn(parts, filter.getColumn());
                parts.add("= ?");
                break;
            case LIKE:
                addColumn(parts, filter.getColumn());
                parts.add("LIKE ?");
                break;
            case GT:
                addColumn(parts, filter.getColumn());
                parts.add("> ?");
                break;
            case GTE:
                addColumn(parts, filter.getColumn());
                parts.add(">= ?");
                break;
            case LT:
                addColumn(parts, filter.getColumn());
                parts.add("< ?");
                break;
            case LTE:
                addColumn(parts, filter.getColumn());
                parts.add("<= ?");
                break;
            default:
                throw new IllegalStateException("Unknown binary filter " + filter.getClause());
            }

            params.add(filter.createFilterParam());
        }

        void columnToSQL(ColumnSTFilter<?> filter, SQLBuilder parts, List<SQLFilterParam<?>> params) {
            switch (filter.getClause()) {
            case NOTNULL:
                addColumn(parts, filter.getColumn());
                parts.add("NOT NULL");
                break;
            case ISNULL:
                addColumn(parts, filter.getColumn());
                parts.add("IS NULL");
                break;
            default:
                throw new IllegalStateException("Unknown column filter " + filter.getClause());
            }
        }

        void addColumn(SQLBuilder parts, Column<?> column) {
            parts.add(columnSQL(column));
        }

        String columnSQL(Column<?> column) {
            String name = column.getName();
            if (useAlias == false)
                return name;
            String alias = sourceToAlias.get(column.record.getTable().getRecordSource());
            if (alias == null) {
                throw new DbException("Invalid reference to column " + name + " not present in the query's tables");
            }
            return alias + "." + name;
        }
    }
}