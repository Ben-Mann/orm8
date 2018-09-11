package net.benmann.orm8.db;

import java.sql.ResultSet;

public interface MapRow<T> {
	T map(ResultSet rs, int row);
}