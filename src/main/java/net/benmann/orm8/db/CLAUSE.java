package net.benmann.orm8.db;

import net.benmann.orm8.db.SingleTableFilter.BinaryColumnSTFilter;
import net.benmann.orm8.db.SingleTableFilter.BinarySTFilter;
import net.benmann.orm8.db.SingleTableFilter.ColumnSTFilter;

public enum CLAUSE {
    GT,
    NOTNULL,
    ISNULL,
    EQ,
    AND,
    LIKE,
    LT,
    GTE,
    LTE;

    public <T> SingleTableFilter getFilter(AbstractSingleTableRecord<?> record, Column<T> column) {
        return new ColumnSTFilter<T>(this, record, column);
    }

    public <T> SingleTableFilter getFilter(AbstractSingleTableRecord<?> record, Column<T> column, T param) {
        return new BinarySTFilter<T>(this, record, column, param);
    }

    public <T> SingleTableFilter getFilter(AbstractSingleTableRecord<?> record, Column<T> column, Column<T> other) {
        return new BinaryColumnSTFilter<T>(this, record, column, other);
    }
}