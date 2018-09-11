package net.benmann.orm8.db;

import java.sql.PreparedStatement;

public class SQLFilterParam<T> {
    final Column<T> column;
    final T value;

    SQLFilterParam(Column<T> column, T value) {
        this.column = column;
        this.value = value;
    }

    void set(PreparedStatement st, int index) {
        column.set(st, index, value);
    }
}