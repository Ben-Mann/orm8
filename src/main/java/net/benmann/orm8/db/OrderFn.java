package net.benmann.orm8.db;

public interface OrderFn<R extends ORM8Record<R>> {
    OrderImpl f(R record);
}