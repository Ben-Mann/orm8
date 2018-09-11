package net.benmann.orm8.db;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import net.benmann.orm8.db.RecordSource.SingleSource;
import net.benmann.orm8.db.VersionTable.VersionRecord;

/**
 * A database table.
 */
public class AbstractTable<T extends AbstractTable<T, R, D>, R extends AbstractSingleTableRecord<R>, D extends DbConnection<D>> extends ORM8Table<T, R, D> {

    protected AbstractTable(D connection, String tableName, Function<T, R> createRecord) {
        super(new SingleSource<T, R, D>(connection, tableName, createRecord));
        connection.addTable(this);
    }

    @Override public List<ORM8Table<?, ?, ?>> allTables() {
        return Arrays.asList(this);
    }

    /**
     * Generate results for this table using arbitrary or vendor specific SQL.
     */
    public AbstractRecordResults<R> select(String sql, Object... params) {
        return new AbstractRecordResults<R>(recordSource.connection.query(sql, params), () -> helper.create());
    }

    /**
     * Get the number of migrations we've already done on the open db
     */
    public int migrated() {
        try {
            String tableName = helper.getTable().getRecordSource().tableName;
            VersionRecord<D> version = getConnection()._versions.where(v -> v.table.is(tableName)).select().get();
            if (version == null)
                return 0;

            return version.version.get();
        } catch (DbException e) {
            //If we're complaining about the version table, we might BE the version table, and need construction. Otherwise, bail.
            if (!(helper instanceof VersionRecord)) {
                e.printStackTrace();
                throw e;
            }
        }
        return 0;
    }

    /**
     * The number of migrations required for this table to bring it from a new db to the current schema.
     */
    public int migrations() {
        //FIXME
        return 1; //creation.
    }

    //    public Query<R> where(ColumnCondition<R> condition) {
    //        helper.getColumns(); // init columns
    //        SingleTableFilter filter = condition.build(helper);
    //        if (filter.getTables().isEmpty())
    //            throw new IllegalStateException("The filter must reference a table.");
    //
    //        return new Query<R>(helper, filter, null);
    //    }


    @Override protected SingleSource<T, R, D> getRecordSource() {
        return (SingleSource<T, R, D>) recordSource;
    }
    

    public static class JoinedTable<
        T extends JoinedTable<T, TA, RA, TB, RB, R, D>,
        TA extends ORM8Table<TA, RA, D>,     
        RA extends ORM8Record<RA>, 
        TB extends ORM8Table<TB, RB, D>, 
        RB extends ORM8Record<RB>, 
        R extends JoinedRecord<R, RA, RB>,
        D extends DbConnection<D>
    > 
    extends ORM8Table<T, R, D>{


        protected JoinedTable(RecordSource<T, R, D> recordSource) {
            super(recordSource);
        }

        @Override public List<ORM8Table<?, ?, ?>> allTables() {
            return Arrays.asList(getRecordSource().left, getRecordSource().right);
        }

        //        @Override public <Q, OT extends ORM8Table<OT, OR, D>, OR extends ORM8Record<OR>> JoinedSource<T, R, OT, OR, D> join(OT other, net.benmann.orm8.db.ORM8Table.JoinColumnFn<Q, R> leftCol, net.benmann.orm8.db.ORM8Table.JoinColumnFn<Q, OR> rightCol) {
        //            // TODO Auto-generated method stub
        //            return null;
        //        }
        
        @Override protected JoinedSource<T, TA, RA, TB, RB, R, D> getRecordSource() {
            return (JoinedSource<T, TA, RA, TB, RB, R, D>) recordSource;
        }

    }

}