package net.benmann.orm8.db;

import java.util.List;

public interface DbStatement {
	<T> T query(DbConnection db, MapSingleResult<T> handler);
	<T> List<T> query(DbConnection db, MapRow<T> handler);
}