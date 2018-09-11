package net.benmann.orm8.db;

import net.benmann.orm8.db.Column.DateColumn;
import net.benmann.orm8.db.Column.IntegerColumn;
import net.benmann.orm8.db.Column.StringColumn;
import net.benmann.orm8.db.VersionTable.VersionRecord;

public class VersionTable<D extends DbConnection<D>> extends AbstractTable<VersionTable<D>, VersionRecord<D>, D> {
    public VersionTable(D connection) {
        super(connection, "__version", VersionRecord<D>::new);
    }

    public static class VersionRecord<D extends DbConnection<D>> extends AbstractSingleTableRecord<VersionRecord<D>> {
        private VersionRecord(VersionTable<D> table) {
            super(table);
        }

        //Our key provides a unique integer range of Ids for all children of a given key.
        public final StringColumn table = fields.stringColumn("key", KeyType.KEY);
        public final IntegerColumn version = fields.integerColumn("id");
        public final DateColumn updated = fields.dateColumn("updated");
    }
}