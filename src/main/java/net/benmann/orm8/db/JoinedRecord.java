package net.benmann.orm8.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.benmann.orm8.db.AbstractTable.JoinedTable;

public class JoinedRecord<R extends JoinedRecord<R, A, B>, A extends ORM8Record<A>, B extends ORM8Record<B>> extends ORM8Record<R> {
    Column<?>[] recordColumnArray = null;
    public A left;
    public B right;

    JoinedRecord(JoinedTable<?, ?, A, ?, B, R, ?> table) {
        super(table);
        left = table.getRecordSource().left.create();
        right = table.getRecordSource().right.create();
    }

    @Override protected JoinedTable<?, ?, A, ?, B, R, ?> getTable() {
        return (JoinedTable<?, ?, A, ?, B, R, ?>) _table;
    }

    //Ultimately these are only definitions - the SQL builder will populate the new record's values.
    @Override public Column<?>[] getColumns() {
        if (recordColumnArray != null)
            return recordColumnArray;

        List<Column<?>> result = new ArrayList<Column<?>>();

        result.addAll(Arrays.asList(left.getColumns()));
        result.addAll(Arrays.asList(right.getColumns()));

        recordColumnArray = result.toArray(new Column<?>[] {});
        return recordColumnArray;
    }
	
}