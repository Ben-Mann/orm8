package net.benmann.orm8.db;

//Must be specialised for joins and single records based on the columns returned by the sqlprovider
public interface ORM8Results<T extends ORM8Record<T>> {
    T get();

    boolean isValid();

    void close();
}