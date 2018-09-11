package net.benmann.orm8.db;

public interface ColumnAggregate<R extends AbstractSingleTableRecord<R>, T> {
    Column<T> get(R record);
}