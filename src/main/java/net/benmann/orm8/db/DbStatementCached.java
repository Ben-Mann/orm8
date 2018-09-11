package net.benmann.orm8.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DbStatementCached implements DbStatement {
	final String sql;
	final String[] params;
	
	DbConnection connection;
	
	public DbStatementCached(String sql, String[] params) {
		this.sql = sql;
		this.params = params;
	}
	
	@Override public <T> T query(DbConnection db, MapSingleResult<T> handler) {
		try (PreparedStatement stmt = db.connection.prepareStatement(sql, params)) {
			if (!stmt.execute())
				return null;
			
			try (ResultSet rs = stmt.getResultSet()) {
				return handler.map(rs);
			}
        } catch (SQLException e) {
        	throw new DbException(e);
		}
	}
	
	@Override public <T> List<T> query(DbConnection db, MapRow<T> handler) {
		try (PreparedStatement stmt = db.connection.prepareStatement(sql, params)) {
    		List<T> result = new ArrayList<T>();
    		if (!stmt.execute())
    			return result;
    		
    		int row = 0;
    		do {
    			try (ResultSet rs = stmt.getResultSet()) {
    				if (rs == null)
    					return result;
    				result.add(handler.map(rs, row++));
    			}
    		} while(true);        		
        } catch (SQLException e) {
        	throw new DbException(e);
		}
	}
}