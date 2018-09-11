package net.benmann.orm8.db;

import net.benmann.orm8.db.ORM8Table.IColumnCondition;

public interface IQuery<R extends ORM8Record<R>> {
    /** Return the count of records matching this query */
    int count();

    /** Delete all records matching this query */
    void delete();

    /** Modifies this query to only return the first result. */
    IQuery<R> first();

    /** Modifies this query to only return the first n results. */
    IQuery<R> first(int i);

    /** Returns the minimum value from the supplied column */
    public <Q> Q min(AggregateColumnFn<Q, R> fn);

    /** Returns the maximum value from the supplied column */
    public <Q> Q max(AggregateColumnFn<Q, R> fn);

    /** Modifies this query to return values in the specified order */
    public IQuery<R> order(OrderFn<R> ordering);

    /** Adds conditions to this query */
    public IQuery<R> where(IColumnCondition<R> condition);

    /** Get an iterable resultset from this query */
    public ORM8Results<R> select();

    /** Get an iterable resultset for specific columns from this query */
    public ORM8Results<R> select(SelectColumns<R> columns);
}