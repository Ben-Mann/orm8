package net.benmann.orm8.db;

public interface ToEnumeratedTypeFn<Q extends EnumeratedType> {
    Q f(Integer i);
}