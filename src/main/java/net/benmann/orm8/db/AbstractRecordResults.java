package net.benmann.orm8.db;

import java.io.Closeable;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A helper for query results. Calling get() does two things - it converts the
 * results to the desired row type, it moves the cursor forward, and it closes a
 * completed recordset.
 * Call get() to get a record. Call isValid() to verify there's more in the result set.
 */
public class AbstractRecordResults<R extends ORM8Record<R>> implements Closeable, ORM8Results<R> {
    final SingleQuery sq; // ResultSet rs;
    final Supplier<R> createRecordFn;

    protected boolean hasNext;
	
    AbstractRecordResults(SingleQuery sq, Supplier<R> createRecordFn) {
        this.sq = sq;
        this.createRecordFn = createRecordFn;
        try {
            hasNext = sq.rs.next();
        } catch (SQLException e) {
            throw new DbException(e);
        }
	}
	
    public boolean isValid() {
        return hasNext;
    }

    public R get() {
        if (!hasNext)
            return null;

        try {
            R result = createRecordFn.get();

            // TODO - update result columns based on available params, in order.
            // columns must come from the SingleQuery, and reflect what we asked
            // for!!
            // TODO - find out if a column's name is in the resultset, before
            // offering to update it.
            ResultSetMetaData rsMetaData = sq.rs.getMetaData();
            int numberOfColumns = rsMetaData.getColumnCount();

            Map<String, Integer> columnMap = new HashMap<String, Integer>();

            // The query result is more likely a subset of the table, so we add that to the map.
            for (int i = 1; i < numberOfColumns + 1; i++) {
                columnMap.put(rsMetaData.getColumnName(i), i);
            }

            for (Column<?> column : result.getColumns()) {
                Integer index = columnMap.get(column.getName());
                if (index == null)
                    continue;
                column.update(sq.rs, index);
            }

            hasNext = sq.rs.next();
            if (!hasNext)
                sq.close();

            return result;
        } catch (SQLException e) {
            throw new DbException(e);
        }
	}

    @Override public void close() {
        sq.close();
    }
}