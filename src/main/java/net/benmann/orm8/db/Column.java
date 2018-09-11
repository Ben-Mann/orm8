package net.benmann.orm8.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class Column<T> {
    /** Column names MUST NOT conflict with db engine internals. The default mechanism to handle this is to always prefix them with db_ */
    public static final String COLUMN_PREFIX = "db_";
    boolean dirty;
    final String name;
    final EnumSet<KeyType> keyType = EnumSet.noneOf(KeyType.class);
    final AbstractSingleTableRecord<?> record;
    public final boolean isNullable;

    ColumnType<T> columnType;
    private T value;

    public static class ColumnFactory {
        public Map<String, Column<?>> columns = new HashMap<>();
        final AbstractSingleTableRecord<?> record;

        ColumnFactory(AbstractSingleTableRecord<?> record) {
            this.record = record;
        }

        private <T extends Column<?>> T add(T column) {
            String key = column.getName();
            if (columns.containsKey(key))
                throw new DbException("The column " + key + " was defined multiple times.");

            columns.put(key, column);
            return column;
        }

        public BooleanColumn booleanColumn(String name, KeyType... kts) {
            return add(new BooleanColumn(record, name, kts));
        }

        public DateColumn dateColumn(String name, KeyType... kts) {
            return add(new DateColumn(record, name, kts));
        }

        public DoubleArrayColumn doubleArrayColumn(String name, KeyType... kts) {
            return add(new DoubleArrayColumn(record, name, kts));
        }

        public DoubleColumn doubleColumn(String name, KeyType... kts) {
            return add(new DoubleColumn(record, name, kts));
        }

        public IntegerColumn integerColumn(String name, KeyType... kts) {
            return add(new IntegerColumn(record, name, kts));
        }

        public UUIDColumn uuidColumn(String name, KeyType... kts) {
            return add(new UUIDColumn(record, name, kts));
        }

        public <Q extends EnumeratedType> EnumColumn<Q> enumColumn(String name, ToEnumeratedTypeFn<Q> fn, KeyType... kts) {
            return add(new EnumColumn<Q>(record, name, fn, kts));
        }

        public LongColumn longColumn(String name, KeyType... kts) {
            return add(new LongColumn(record, name, kts));
        }

        public StringColumn stringColumn(String name, KeyType... kts) {
            return add(new StringColumn(record, name, kts));
        }
    }

    //XXX not a table.
    //    void setTable(AbstractSingleTableRecord<?> record) {
    //        this.record = record;
    //    }

    
    public static class EnumColumn<Q extends EnumeratedType> extends Column<Q> {
        private EnumColumn(AbstractSingleTableRecord<?> record, String name, ToEnumeratedTypeFn<Q> fn, KeyType... keyTypes) {
            super(record, ColumnType.createEnum(fn), name, keyTypes);
        }
    }
    
    public static class IntegerColumn extends Column<Integer> {
        private IntegerColumn(AbstractSingleTableRecord<?> record, String name, KeyType... keyTypes) {
            super(record, ColumnType.INTEGER, name, keyTypes);
        }
    }

    public static class UUIDColumn extends Column<UUID> {
        private UUIDColumn(AbstractSingleTableRecord<?> record, String name, KeyType... keyTypes) {
            super(record, ColumnType.UUID, name, keyTypes);
        }
    }

    public static class DoubleColumn extends Column<Double> {
        private DoubleColumn(AbstractSingleTableRecord<?> record, String name, KeyType... keyTypes) {
            super(record, ColumnType.DOUBLE, name, keyTypes);
        }
    }

    public static class DoubleArrayColumn extends Column<double[]> {
        private DoubleArrayColumn(AbstractSingleTableRecord<?> record, String name, KeyType... keyTypes) {
            super(record, ColumnType.DOUBLE_ARRAY, name, keyTypes);
        }
    }

    public static class LongColumn extends Column<Long> {
        private LongColumn(AbstractSingleTableRecord<?> record, String name, KeyType... keyTypes) {
            super(record, ColumnType.LONG, name, keyTypes);
        }
    }

    public static class StringColumn extends Column<String> {
        private StringColumn(AbstractSingleTableRecord<?> record, String name, KeyType... keyTypes) {
            super(record, ColumnType.STRING, name, keyTypes);
        }

        public SingleTableFilter like(String t) {
            return CLAUSE.LIKE.getFilter(record, this, t);
        }
    }

    public static class DateColumn extends Column<Date> {
        private DateColumn(AbstractSingleTableRecord<?> record, String name, KeyType... keyTypes) {
            super(record, ColumnType.DATE, name, keyTypes);
        }
    }

    public static class BooleanColumn extends Column<Boolean> {
        private BooleanColumn(AbstractSingleTableRecord<?> record, String name, KeyType... keyTypes) {
            super(record, ColumnType.BOOLEAN, name, keyTypes);
        }
    }

    // TODO support other functionality, including query operations.
    /** Allow a Column<Q> to be treated as a Column<T> in many operations */
    public static class AccessColumn<T, Q> {
        private final Column<Q> concreteColumn;
        private final Function<Q, T> getAccess;
        private final BiConsumer<Column<Q>, T> setAccess;

        public AccessColumn(Column<Q> concreteColumn, Function<Q, T> getAccess, BiConsumer<Column<Q>, T> setAccess) {
            this.concreteColumn = concreteColumn;
            this.getAccess = getAccess;
            this.setAccess = setAccess;
        }

        public T get() {
            return getAccess.apply(concreteColumn.get());
        }

        public void set(T value) {
            setAccess.accept(concreteColumn, value);
        }
    }
    
    private Column(AbstractSingleTableRecord<?> record, ColumnType<T> columnType, String name, KeyType... keyType) {
        this.record = record;
        this.columnType = columnType;
        this.name = COLUMN_PREFIX + name;
        for (KeyType kt : keyType) {
            this.keyType.add(kt);
        }
        isNullable = this.keyType.contains(KeyType.NULLABLE);
    }

    public String getName() {
        return name;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void set(Column<T> value) {
        set(value.get());
    }

    /**
     * Set a new value. Marks the column dirty.
     */
    public void set(T value) {
        if (value == null && this.value == null)
            return;
        if (value != null && value.equals(this.value))
            return;
        this.value = value;
        dirty = true;
    }

    /**
     * Get the current value / most recently set value.
     */
    public T get() {
        //TODO if we're using the builder's lambda, get is illegal.
        return value;
    }

    public void flagClean() {
        dirty = false;
    }

    /**
     * Package visible. Update the column value as the result of a db action.
     */
    void update(ResultSet rs, int index) {
        try {
            columnType.update(rs, this, index);
            flagClean();
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    /**
     * Set the statement parameter 'index' with the value from this column.
     */
    public void set(PreparedStatement ps, int index) {
        try {
            columnType.set(ps, this, index);
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    public void set(PreparedStatement ps, int index, T value) {
        try {
            columnType.set(ps, index, value);
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    public void setKey(Object o) {
        try {
            columnType.setKey(o, this);
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    public SingleTableFilter greaterThan(Column<T> t) {
        return greaterThan(t.get());
    }

    public SingleTableFilter greaterThan(T t) {
        return CLAUSE.GT.getFilter(record, this, t);
    }
    
    public SingleTableFilter gt(Column<T> t) {
        return gt(t.get());
    }

    public SingleTableFilter gt(T t) {
        return CLAUSE.GT.getFilter(record, this, t);
    }

    public SingleTableFilter lessThan(Column<T> t) {
        return lessThan(t.get());
    }

    public SingleTableFilter lessThan(T t) {
        return CLAUSE.LT.getFilter(record, this, t);
    }

    public SingleTableFilter lt(Column<T> t) {
        return lt(t.get());
    }

    public SingleTableFilter lt(T t) {
        return CLAUSE.LT.getFilter(record, this, t);
    }

    public SingleTableFilter greaterThanOrEqual(Column<T> t) {
        return greaterThanOrEqual(t.get());
    }

    public SingleTableFilter greaterThanOrEqual(T t) {
        return CLAUSE.GTE.getFilter(record, this, t);
    }

    public SingleTableFilter gte(Column<T> t) {
        return gte(t.get());
    }

    public SingleTableFilter gte(T t) {
        return CLAUSE.GTE.getFilter(record, this, t);
    }

    public SingleTableFilter lessThanOrEqual(Column<T> t) {
        return lessThanOrEqual(t.get());
    }

    public SingleTableFilter lessThanOrEqual(T t) {
        return CLAUSE.LTE.getFilter(record, this, t);
    }

    public SingleTableFilter lte(Column<T> t) {
        return lte(t.get());
    }

    public SingleTableFilter lte(T t) {
        return CLAUSE.LTE.getFilter(record, this, t);
    }

    public SingleTableFilter is(T t) {
        if (t == null) {
            return isNull();
        }
    	return CLAUSE.EQ.getFilter(record, this, t);
    }

    public SingleTableFilter is(Column<T> t) {
        //TODO Is Column<T> using the lambda helper? If so, we want the column filter  
        return is(t.get());
    }

    //    public SingleTableFilter is(Column<?> other) {
    //        return CLAUSE.EQ.getFilter(table, this, other);
    //    }

    public SingleTableFilter notNull() {
        return CLAUSE.NOTNULL.getFilter(record, this);
    }

    public SingleTableFilter isNull() {
        return CLAUSE.ISNULL.getFilter(record, this);
    }

    @Override public String toString() {
        return String.valueOf(value);
    }
}
