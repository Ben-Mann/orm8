package net.benmann.orm8.db;

import java.util.function.Function;

import net.benmann.orm8.db.AbstractTable.JoinedTable;
import net.benmann.orm8.db.ORM8Table.ColumnCondition;

//<T extends AbstractTable<T, R, D>, R extends AbstractRecord<T, R, D>, D extends DbConnection<D>>
public class JoinedSource<
T extends JoinedTable<T, TA, RA, TB, RB, R, D>,
TA extends ORM8Table<TA, RA, D>,     
RA extends ORM8Record<RA>, 
TB extends ORM8Table<TB, RB, D>, 
RB extends ORM8Record<RB>, 
R extends JoinedRecord<R, RA, RB>,
D extends DbConnection<D>
> extends RecordSource<T, R, D> {
    public final TA left;
    public final TB right;
    final ColumnCondition<R> condition;

    JoinedSource(D connection, TA left, TB right, Function<T, R> createRecord, ColumnCondition<R> condition) {
        super(connection, createRecord);
        this.left = left;
        this.right = right;
        this.condition = condition;
    }

    //    //TODO move a bunch of stuff to interfaces, and then rely on those. Query. Results. Record.
    //    JoinedQuery<?, D> where(AbstractTable.JoinedColumnCondition c) {
    //
    //    }


}