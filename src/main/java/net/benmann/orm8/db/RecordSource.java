package net.benmann.orm8.db;

import java.util.function.Function;

public abstract class RecordSource<T extends ORM8Table<T, R, D>, R extends ORM8Record<R>, D extends DbConnection<D>> {
    protected final Function<T, R> createRecord;
    protected final D connection;

    RecordSource(D connection, Function<T, R> createRecord) {
        this.connection = connection;
        this.createRecord = createRecord;
    }

    public static class SingleSource<T extends ORM8Table<T, R, D>, R extends ORM8Record<R>, D extends DbConnection<D>> extends RecordSource<T, R, D> {
        final String tableName;

        public SingleSource(D connection, String tableName, Function<T, R> createRecord) {
            super(connection, createRecord);
            this.tableName = tableName;
        }


    }
}
