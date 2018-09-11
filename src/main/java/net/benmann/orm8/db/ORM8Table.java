package net.benmann.orm8.db;

import java.util.List;

import net.benmann.orm8.db.AbstractTable.JoinedTable;

//FIXME recordsource as a generic argument.
public abstract class ORM8Table<T extends ORM8Table<T, R, D>, R extends ORM8Record<R>, D extends DbConnection<D>> {
    protected final RecordSource<T, R, D> recordSource;
    protected final R helper;

    protected ORM8Table(RecordSource<T, R, D> recordSource) {
        this.recordSource = recordSource;
        helper = recordSource.createRecord.apply(table());
    }

    abstract public List<ORM8Table<?, ?, ?>> allTables();

    public static interface JoinColumnFn<Q, V extends ORM8Record<V>> {
        Column<Q> get(V table);
    }

    @SuppressWarnings("unchecked") protected T table() {
        return (T) this;
    }

    protected D getConnection() {
        return recordSource.connection;
    }

    public R create() {
        return recordSource.createRecord.apply(table()); //helper._factory.createRow(helper.connection);
    }

    /**
     * The default, * query.
     */
    public Query<R> all() {
        return new Query<R>(helper, null, null);
    }

    public int count() {
        return all().count();
    }

    /**
     * Return a table specific query
     */
    public Query<R> where(ColumnCondition<R> condition) {
        helper.getColumns(); // init columns
        SingleTableFilter filter = condition.build(helper);
        if (filter.getTable() == null)
            throw new IllegalStateException("The filter must reference a table.");

        return new Query<R>(helper, filter, null);
    }

    /**
     * TODO
     * Syntax is
     * db.table1.join(db.table2, t1 -> t1.id, t2 -> t2.id).where(t1_and_t2 -> t1_and_t2.left.table1_column.is(a).and(t1_and_t2.right.table2_column.is(b))).select().get();
     * Note that the return type is a joined abstract record, so has elements from each.
     * The joins may be in a tree - result.left.left.column, for example.
     * 
     * @return
     */
    //    public abstract <
    //        Q, 
    //        JT extends JoinedTable<JT, T, R, OT, OR, JR, D>, 
    //        JR extends JoinedRecord<JR, R, OR>, 
    //        OT extends ORM8Table<OT, OR, D>, 
    //        OR extends ORM8Record<OR>
    //    > JoinedTable<JT, T, R, OT, OR, JR, D> join(OT other, ColumnCondition<JR> condition); //JoinColumnFn<Q, R> leftCol, JoinColumnFn<Q, OR> rightCol);
    //    
    //    /**
    //     * 
    //     */
    public <
        Q, 
        JT extends JoinedTable<JT, T, R, OT, OR, JR, D>, 
        JR extends JoinedRecord<JR, R, OR>, 
        OT extends ORM8Table<OT, OR, D>, 
        OR extends ORM8Record<OR>
    > JoinedTable<JT, T, R, OT, OR, JR, D> join(OT other, ColumnCondition<JR> condition) { //JoinColumnFn<Q, R> leftCol, JoinColumnFn<Q, OR> rightCol) {
        //Ruh roh. The record create method needs an originating RECORD, not a table?
        return new JoinedTable(new JoinedSource(this.getConnection(), this, other, t -> new JoinedRecord<JR, R, OR>((JT) t), condition));
    }

    //public <A, OT extends ORM8Table<OT, OR, D>, OR extends ORM8Record<OR>> JoinedTable<T, R, OT, OR, D> join(OT other, Function<R, A> leftCol, Function<OR, A> rightCol);

    public static interface IColumnCondition<T extends ORM8Record<T>> {
        SingleTableFilter build(T b);
    }

    public static interface ColumnCondition<V extends ORM8Record<V>> extends IColumnCondition<V> {
        SingleTableFilter build(V b);
    }

    public static interface JoinedColumnCondition {
        SingleTableFilter build(JoinedSource b);
    }

    /** Returns the source (a db table, or a join) for this virtual table */
    protected RecordSource<T, R, D> getRecordSource() {
        return recordSource;
    }
}