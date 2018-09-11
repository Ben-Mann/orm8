package net.benmann.orm8.db;

public class SQLFilterColumnParam<T> {
    final Column<T> column;
    final Column<T> other;

    SQLFilterColumnParam(Column<T> column, Column<T> other) {
        this.column = column;
        this.other = other;
    }

    //        void set(PreparedStatement st, int index) {
    //            column.set(st, index, other);
    //        }
}