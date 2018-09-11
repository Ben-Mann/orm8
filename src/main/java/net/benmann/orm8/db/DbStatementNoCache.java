package net.benmann.orm8.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DbStatementNoCache implements DbStatement {
    final String sql;

    DbConnection connection;

    public DbStatementNoCache(String sql) {
        this.sql = sql;
    }

    @Override public <T> T query(DbConnection db, MapSingleResult<T> handler) {
        try (Statement stmt = db.connection.createStatement()) {
            if (!stmt.execute(sql))
                return null;

            try (ResultSet rs = stmt.getResultSet()) {
                return handler.map(rs);
            }
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    @Override public <T> List<T> query(DbConnection db, MapRow<T> handler) {
        try (Statement stmt = db.connection.createStatement()) {
            List<T> result = new ArrayList<T>();
            if (!stmt.execute(sql))
                return result;

            int row = 0;
            do {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs == null)
                        break;
                    result.add(handler.map(rs, row++));
                }
            } while (true);
            return result;
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }
}