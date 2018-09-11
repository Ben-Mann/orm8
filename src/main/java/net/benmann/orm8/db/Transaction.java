package net.benmann.orm8.db;

/**
 * TODO
 * Executes an SQL BEGIN TRANSACTION; enclosing ORM8 commands then run in this context; executes an END TRANSACTION
 * at the end of the block.
 * 
 * Likely we will move on to a different pattern eventually though, such as:
 * 
 * <pre>
 * db.withTransaction(() -> {
 *     try {
 *         db.doSomething();
 *         db.doSomethingElse();
 *     } catch (SillinessError e) {
 *         return Transaction.ROLLBACK;
 *     }
 *     return Transaction.COMMIT;
 * });
 * </pre>
 * 
 * which may be a better compile-time story than the following
 * 
 * <pre>
 * try (Transaction t = db.createTransaction()) {
 *     db.doSomething();
 *     db.doSomethingElse();
 *     t.commit();
 * } catch (SillinessError e) {
 *     t.rollback();
 * }
 * </pre>
 * 
 * which cannot ensure you actually called commit or rollback until runtime.
 * 
 * TODO transactions started within this one should become part of the enclosing transaction (and manage state accordingly)
 */
public class Transaction implements AutoCloseable {
    public enum Result {
        COMMIT, ROLLBACK, UNKNOWN
    }

    protected Result result = Result.UNKNOWN;

    public Transaction(DbConnection<?> connection) {
    }

    public void setResult(Result r) {
        if (result != Result.UNKNOWN)
            throw new IllegalStateException("Attempt to change transaction state from " + result + " to " + r);
        result = r;
    }

    public void commit() {
        setResult(Result.COMMIT);
    }

    public void rollback() {
        setResult(Result.ROLLBACK);
    }

    //If we close this object and you haven't called commit or rollback, it's considered a programming error.
    @Override public void close() {
        if (result == Result.UNKNOWN)
            throw new IllegalStateException("The transaction was not closed.");
    }

    //If the transaction is garbage collected, run close.
    @Override public void finalize() {
        close();
    }
}