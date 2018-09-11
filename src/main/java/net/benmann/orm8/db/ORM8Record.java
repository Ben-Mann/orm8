package net.benmann.orm8.db;

import java.util.Collections;
import java.util.List;

public abstract class ORM8Record<R extends ORM8Record<R>> {
    protected final ORM8Table<?, R, ?> _table;

    protected ORM8Record(ORM8Table<?, R, ?> table) {
        this._table = table;
    }

    public DbConnection<?> getConnection() {
        return getTable().getConnection();
    }

    public static abstract class RecordSources {
        /** Get all contributing db tables */
        public abstract List<AbstractTable<?, ?, ?>> allTables();

        /** Get the source(s) for this record */
        public abstract List<ORM8Table<?, ?, ?>> contributors();

        public static class SingleRecordTable extends RecordSources {
            final ORM8Table<?, ?, ?> source;

            SingleRecordTable(ORM8Table<?, ?, ?> source) {
                this.source = source;
            }

            @Override public List<AbstractTable<?, ?, ?>> allTables() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override public List<ORM8Table<?, ?, ?>> contributors() {
                return Collections.singletonList(source);
            }

        }
    }

    //    /** Get all tables referenced by this record. */
    //    abstract protected RecordSources getTables();
    abstract protected ORM8Table<?, R, ?> getTable();

    abstract public Column<?>[] getColumns();
}