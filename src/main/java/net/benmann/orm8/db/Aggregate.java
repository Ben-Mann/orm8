package net.benmann.orm8.db;


public class Aggregate<T> {
    public enum Fn {
        MIN,
        MAX,
        AVG,
        SUM
    }

    final Column<T> column;
    final Aggregate.Fn fn;

    Aggregate(Aggregate.Fn fn, Column<T> column) {
        this.column = column;
        this.fn = fn;
    }
	
    public static <Q> Aggregate<Q> min(Column<Q> column) {
        return new Aggregate<Q>(Fn.MIN, column);
    }

    public static <Q> Aggregate<Q> max(Column<Q> column) {
        return new Aggregate<Q>(Fn.MAX, column);
    }
}