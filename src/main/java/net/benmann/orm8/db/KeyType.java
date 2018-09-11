package net.benmann.orm8.db;

import java.util.Arrays;
import java.util.List;

public enum KeyType {
    KEY,
    AUTOINCREMENT,
    NULLABLE;

    private static <T> List<T> list(T... k) {
        return Arrays.asList(k);
    }

    static List<KeyType> keys = list(KEY, AUTOINCREMENT);
}