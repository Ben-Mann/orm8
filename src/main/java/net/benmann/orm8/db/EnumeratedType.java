package net.benmann.orm8.db;

import java.util.Map;

public interface EnumeratedType {
    int getEnumeration();

    public static <P extends Enum<?> & EnumeratedType> P fromId(int id, P[] values, Map<Integer, P> valueMap) {
        synchronized (valueMap) {
            if (!valueMap.isEmpty())
                return valueMap.get(id);
            for (P t : values) {
                valueMap.put(t.getEnumeration(), t);
            }
            return valueMap.get(id);
        }
    }

}