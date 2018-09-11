package net.benmann.orm8.db;

import java.util.ArrayList;
import java.util.List;

//FIXME rename
public class OrderImpl {
    public final List<OrderImpl.ColumnOrder> columns = new ArrayList<>();

    public static class ColumnOrder {
        public final Column<?> column;
        public final OrderDirection direction;

        private ColumnOrder(OrderDirection direction, Column<?> column) {
            this.direction = direction;
            this.column = column;
        }
    }

    public static class Order {
        public static OrderImpl asc(Column<?> column0, Column<?>... columns) {
            return ascending(column0, columns);
        }

        public static OrderImpl desc(Column<?> column0, Column<?>... columns) {
            return descending(column0, columns);
        }

        public static OrderImpl ascending(Column<?> column0, Column<?>... columns) {
            return new OrderImpl(OrderDirection.ASCENDING, column0, columns);
        }

        public static OrderImpl descending(Column<?> column0, Column<?>... columns) {
            return new OrderImpl(OrderDirection.DESCENDING, column0, columns);
        }

    }

    private OrderImpl(OrderDirection direction, Column<?> column0, Column<?>... newColumns) {
        add(direction, column0, newColumns);
    }

    private OrderImpl add(OrderDirection direction, Column<?> column0, Column<?>... newColumns) {
        columns.add(new ColumnOrder(direction, column0));

        if (columns != null) {
            for (Column<?> column : newColumns) {
                columns.add(new ColumnOrder(direction, column));
            }
        }

        return this;
    }


    public OrderImpl asc(Column<?> column0, Column<?>... columns) {
        return ascending(column0, columns);
    }

    public OrderImpl desc(Column<?> column0, Column<?>... columns) {
        return descending(column0, columns);
    }

    public OrderImpl ascending(Column<?> column0, Column<?>... columns) {
        return add(OrderDirection.ASCENDING, column0, columns);
    }
    
    public OrderImpl descending(Column<?> column0, Column<?>... columns) {
        return add(OrderDirection.DESCENDING, column0, columns);
    }
}