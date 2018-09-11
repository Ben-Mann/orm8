package net.benmann.orm8.db;

import java.sql.ResultSet;

public interface MapSingleResult<T> {
	T map(ResultSet rs);
}