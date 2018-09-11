package net.benmann.orm8.db;

public interface AggregateColumnFn<Q, R extends ORM8Record<R>> {
    Column<Q> get(R record);
}