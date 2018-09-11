package net.benmann.orm8.db;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.benmann.orm8.db.Column.ColumnFactory;

//T extends AbstractTable<T, R, D>, 
public abstract class AbstractSingleTableRecord<R extends AbstractSingleTableRecord<R>> extends ORM8Record<R> {
    private Column<?>[] recordColumnArray = null;
    protected ColumnFactory fields = new ColumnFactory(this);

    protected AbstractSingleTableRecord(AbstractTable<?, R, ?> table) {
        super(table);
    }

    /** Copy src to dest, explicitly casting src to work around generics issues */
    @SuppressWarnings("unchecked") private <A> void copyColumn(Column<A> dest, Column<?> src) {
        dest.set((A) src.get());
    }

    @Override protected AbstractTable<?, R, ?> getTable() {
        return (AbstractTable<?, R, ?>) _table;
    }

    /**
     * Copy this object's key fields only
     */
    public R copyKey() {
        R result = create();

        Column<?>[] srcColumns = getColumns();
        Column<?>[] destColumns = result.getColumns();
        for (int c = 0; c < srcColumns.length; c++) {
            if (Collections.disjoint(srcColumns[c].keyType, KeyType.keys))
                continue;

            copyColumn(destColumns[c], srcColumns[c]);
        }

        return result;
    }

    public R create() {
        return _table.create();
    }

    public SelectedColumns only(Column<?>... columns) {
        return new SelectedColumns(columns);
    }

    public synchronized Column<?>[] getColumns() {
        if (recordColumnArray != null)
            return recordColumnArray;

        recordColumnArray = setColumns();
        return recordColumnArray;
    }

    //    public String tableName() {
    //        return _table.tableName;
    //    }

    //FIXME this may no longer be required.
    private Column<?>[] setColumns() {
        List<Column<?>> result = new ArrayList<Column<?>>();
        for (Column<?> column : fields.columns.values()) {
            result.add((Column<?>) column);
            //            column.setTable(this);
        }
        return result.toArray(new Column<?>[] {});
    }

    protected Object[] getParams(Column<?>... columns) {
        Object[] result = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            result[i] = columns[i];
        }
        return result;
    }

    protected void flagAllClean() {
        for (Column<?> column : getColumns()) {
            column.flagClean();
        }
    }

    protected R getAll(ResultSet rs) {
        // iterate over columns, and set all of them.
        for (Column<?> column : getColumns()) {
            column.update(rs, -1);
        }
        return record();
    }

    @SuppressWarnings("unchecked")
    private R record() {
        return (R) this;
    }

    /**
     * Insert the whole record - apart from any autoincrement.
     */
    //Here's the million millisecond question: Isn't this query the same EVERY TIME? So calculate it once - and never do so again.
    static class CachedRecordData {
        static int nextStatementKey = 0;

        String insertSql = null;
        String[] insertColumnIndexes = null;
        int[] insertParams = null;
        int[] insertKeys = null;
        int insertStatementKey = nextStatementKey++;
    }

    /** If the record was created using create(), will call insert(); if retrieved from the db, will call update() */
    public void insertOrUpdate() {

    }

    public void insert() {
        getConnection().createBuilder().insert(this);
        flagAllClean();
    }

    /**
     * Should be able to update that record based on what's dirty
     */
    public boolean update() {
        getConnection().createBuilder().update(this);
        flagAllClean();
        return true;
    }
    
    public void delete() {
        getConnection().createBuilder().delete((R) this);
        flagAllClean();
    }
}