package net.benmann.orm8.db;

public abstract class SingleTableFilter {
    public static class ColumnSTFilter<T> extends SingleTableFilter {
        protected AbstractSingleTableRecord<?> record;
        protected Column<T> column;

        public ColumnSTFilter(CLAUSE clause, AbstractSingleTableRecord<?> record, Column<T> column) {
            super(clause);
            this.record = record;
            this.column = column;
        }

        public Column<T> getColumn() {
        	return column;
        }
        
        @Override public ORM8Table<?, ?, ?> getTable() {
            return record.getTable();
        }
    }

    public static class BinarySTFilter<T> extends ColumnSTFilter<T> {
        protected T param;

        public BinarySTFilter(CLAUSE clause, AbstractSingleTableRecord<?> record, Column<T> column, T param) {
            super(clause, record, column);
            this.param = param;
        }
        
        public T getParam() {
        	return param;
        }

        SQLFilterParam<T> createFilterParam() {
            return new SQLFilterParam<T>(getColumn(), getParam());
        }
    }

    public static class BinaryColumnSTFilter<T> extends ColumnSTFilter<T> {
        protected Column<T> other;

        public BinaryColumnSTFilter(CLAUSE clause, AbstractSingleTableRecord<?> record, Column<T> column, Column<T> other) {
            super(clause, record, column);
            this.other = other;
        }

        public Column<T> getOther() {
            return other;
        }

        SQLFilterColumnParam<T> createFilterParam() {
            return new SQLFilterColumnParam<T>(getColumn(), getOther());
        }
    }

    public static class LogicSTFilter extends SingleTableFilter {
    	protected SingleTableFilter a;
    	protected SingleTableFilter b;

        public LogicSTFilter(CLAUSE clause, SingleTableFilter a, SingleTableFilter b) {
            super(clause);
            this.a = a;
            this.b = b;
        }

        @Override public ORM8Table<?, ?, ?> getTable() {
            //Groan. So ta.join(tb.join(tc)).where((a,b,c) -> a.v.is(b.v).and(c.v.is(b.v))) needs to be possible,
            //and the logic therefore spans all the tables within the source.
            //But it could be ta.join(tb.join(tc)).where(j -> j.left.v.is(j.right.left.v) and ...) in which case it
            //really is one table. And this has the advantage that it's obvious which part of the join to use, perhaps.
            ORM8Table<?, ?, ?> ta = a.getTable();
            if (ta == null)
                return null;
            if (b.getTable() != ta)
                return null;
            return ta;
        }
        
        public SingleTableFilter getLeft() {
        	return a;
        }
        
        public SingleTableFilter getRight() {
        	return b;
        }
    }

    protected final CLAUSE clause;

    public SingleTableFilter(CLAUSE clause) {
        this.clause = clause;
    }
    
    public final CLAUSE getClause() {
    	return clause;
    }

    //    public abstract String getSingleTableName();
    public abstract ORM8Table<?, ?, ?> getTable();

    public SingleTableFilter and(SingleTableFilter other) {
        return new LogicSTFilter(CLAUSE.AND, this, other);
    }

    public SingleTableFilter and(SingleTableFilter other1, SingleTableFilter other2, SingleTableFilter... others) {
        SingleTableFilter result = new LogicSTFilter(CLAUSE.AND, this, other1);
        result = result.and(other2);
        if (others != null) {
            for (SingleTableFilter other : others) {
                result = result.and(other);
            }
        }
        return result;
    }
}