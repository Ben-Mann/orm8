package net.benmann.orm8.db;

import java.sql.SQLException;

import net.benmann.orm8.db.ORM8Table.IColumnCondition;

//TODO - most of these should be in AbstractTable. Should AbstractTable be a Query, by default?
public class Query<R extends ORM8Record<R>> implements IQuery<R> {
    final R helper;
    final SingleTableFilter where;
    final OrderFn<R> order;
    Integer top = null;

    /**
     * Construct an all-inclusive query
     */
    public Query(R helper, SingleTableFilter where, OrderFn<R> order) {
        this.where = where;
        this.helper = helper;
        this.order = order;
    }
    
	/**
	 * Equivalent to SELECT COUNT(*)
	 */
    @Override public int count() {
        return helper.getConnection().createBuilder().countAll(helper, where);
	}
	
    @Override public void delete() {
        helper.getConnection().createBuilder().delete(helper, where);
	}
	
	/**
	 * Modifies this query to only return the first result.
	 */
    @Override public Query<R> first() {
		return first(1);
	}
	
	/**
	 * Modifies this query to only return the first n results.
	 */
    @Override public Query<R> first(int n) {
		top = n;
		return this;
	}
	
    @Override public ORM8Results<R> select() {
        return new AbstractRecordResults<R>(helper.getConnection().createBuilder().query(where, helper, top, null, order), () -> helper.getTable().create());
	}
	
    @Override public AbstractRecordResults<R> select(SelectColumns<R> columns) {
        return new AbstractRecordResults<R>(helper.getConnection().createBuilder().query(where, helper, top, columns, order), () -> helper.getTable().create());
	}
	
    // ????
    private <Q> SelectColumns<R> aggregateColumn(Aggregate.Fn f, AggregateColumnFn<Q, R> fn) {
        return new SelectColumns<R>() {
            @Override public SelectedColumns get(R table) {
                return new SelectedColumns(new Aggregate<Q>(f, fn.get(helper)));
            }
        };
    }

    /**
     * FIXME aggregates don't really work this way. They should be added as a
     * returned column and we get the value type-safely from that.
     */
    @SuppressWarnings("unchecked")
    private <Q> Q getAggregateResult(AbstractRecordResults<R> r) {
        try {
            return (Q) r.sq.rs.getObject(1); // FIXME this sucks
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    @Override public <Q> Q min(AggregateColumnFn<Q, R> fn) {
        try (AbstractRecordResults<R> results = new AbstractRecordResults<R>(helper.getConnection().createBuilder().query(where, helper, top, aggregateColumn(Aggregate.Fn.MIN, fn), order), () -> helper.getTable().create())) {
            return getAggregateResult(results);
        }
	}
	
    public <Q> Q max(AggregateColumnFn<Q, R> fn) {
        try (AbstractRecordResults<R> results = new AbstractRecordResults<R>(helper.getConnection().createBuilder().query(where, helper, top, aggregateColumn(Aggregate.Fn.MAX, fn), order), () -> helper.getTable().create())) {
            return getAggregateResult(results);
        }
	}

    @Override public Query<R> order(OrderFn<R> ordering) {
        return new Query<R>(helper, where, ordering);
    }
    
    /**
     * Return a table specific query
     */
    @Override public Query<R> where(IColumnCondition<R> condition) {
        return new Query<R>(helper, where.and(condition.build(helper)), null);
    }


}