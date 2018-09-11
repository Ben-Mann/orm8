package net.benmann.orm8.db;

import java.io.Closeable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SingleQuery implements Closeable {
    final PreparedStatement ps;
    public final ResultSet rs;

    public SingleQuery(PreparedStatement ps) {
        this.ps = ps;
        try {
            this.rs = ps.getResultSet();
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    @Override public void close() {
        try {
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }
}