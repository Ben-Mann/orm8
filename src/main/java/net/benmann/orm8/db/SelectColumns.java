package net.benmann.orm8.db;


public interface SelectColumns<T> {
    SelectedColumns get(T table);
}