package net.benmann.orm8.db.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.benmann.orm8.db.AbstractSingleTableRecord;
import net.benmann.orm8.db.AbstractTable;
import net.benmann.orm8.db.Column.IntegerColumn;
import net.benmann.orm8.db.Column.StringColumn;
import net.benmann.orm8.db.DbConnection;
import net.benmann.orm8.db.KeyType;
import net.benmann.orm8.db.ORM8Results;
import net.benmann.orm8.db.OrderImpl.Order;
import net.benmann.orm8.db.SingleQuery;

public class NoQLTest {
    static final String filename = "./test.db";
    MyDbConnection db;
    File testDirectory;

    private File createTempDirectory() {
        UUID uuid = UUID.randomUUID();
        File directory = new File(FileUtils.getTempDirectory(), uuid.toString());
        directory.mkdir();
        return directory;
    }

    private static void delete() throws IOException {
        Path path = new File(filename).toPath();
        if (!Files.exists(path))
            return;

        Files.delete(path);
    }

    private MyDbConnection getDb() {
        try {
            delete();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        File file = new File(testDirectory, filename);
        System.out.println("Running test on " + file);

        return MyDbConnection.create("jdbc:sqlite:" + file.getAbsolutePath());
    }

    @Before public void setup() {
        testDirectory = createTempDirectory();
        db = getDb();
    }
    
    @After public void teardown() throws IOException, SQLException {
        db.close();
        FileUtils.deleteDirectory(testDirectory);
    }

    // q.and(q.greaterThan(t.id, 0), q.notNull(t.email))
    @Test public void testEmptyDbIsEmpty() {
        assertEquals(0, db.sprockets.where((t) -> t.id.greaterThan(0).and(t.email.notNull())).count());
    }

    @Test public void testInsertRecords() {
        // Insert a record.
        {
            Sprocket sprocket = db.sprockets.create();
            sprocket.email.set("bob@test.com");
            sprocket.insert();
            assertEquals((Integer) 1, sprocket.id.get());
        }

        // Insert another record.
        {
            Sprocket sprocket = db.sprockets.create();
            sprocket.email.set("betty@test.com");
            sprocket.insert();
            assertEquals((Integer) 2, sprocket.id.get());
        }

        // Check there are two records
        assertEquals(2, db.sprockets.count());

        {
            Sprocket sprocket = db.sprockets.create();
            sprocket.email.set("bingo@test.com");
            sprocket.insert();
            assertEquals((Integer) 3, sprocket.id.get());
        }

        assertEquals(3, db.sprockets.count());
    }

    // FIXME. If you join, you will get results from two tables; for some
    // criteria you get values from no tables (count(*)).
    // How do you get these values out?
    // db.Nuts.join(db.Bolts).first();
    // would give you NutsAndBolts... but what is that? It's not really oo...
    // JoinResult, maybe, with _first<Nuts>(), _second<Bolts>()?
    /**
     * Anything not supported by the API must be done with arbitrary SQL.
     * @throws SQLException 
     */
    @Test public void testArbitrarySQL() throws SQLException {
        testInsertRecords();

        db.exec("INSERT INTO widgets (db_id, db_xtable, db_name) VALUES (1, 0, 'box')");
        db.exec("INSERT INTO widgets (db_id, db_xtable, db_name) VALUES (2, 1, 'window')");
        db.exec("INSERT INTO widgets (db_id, db_xtable, db_name) VALUES (3, 2, 'button')");
        db.exec("INSERT INTO widgets (db_id, db_xtable, db_name) VALUES (4, 1, 'box')");
        db.exec("INSERT INTO widgets (db_id, db_xtable, db_name) VALUES (5, 2, 'bar')");
        SingleQuery result = db.query("SELECT t.db_email, w.db_name FROM tables t INNER JOIN widgets w ON t.db_id = w.db_xtable WHERE t.db_id == ? ORDER BY w.db_id", 2);
        assertNotNull(result);

        // Arbitrary queries don't automatically advance the recordset.
        assertTrue(result.rs.next());

        assertEquals("betty@test.com", result.rs.getString(1));
        assertEquals("button", result.rs.getString(2));
        assertTrue(result.rs.next());
        assertEquals("betty@test.com", result.rs.getString(1));
        assertEquals("bar", result.rs.getString(2));
        assertFalse(result.rs.next());
    }

    @Test public void testOrderBy() {
        testInsertRecords();
        {
            Sprocket sprockets = db.sprockets.all().order(t -> Order.asc(t.id)).select().get();
            assertEquals((Integer) 1, sprockets.id.get());
        }
        {
            Sprocket sprockets = db.sprockets.all().order(t -> Order.desc(t.id)).select().get();
            assertEquals((Integer) 3, sprockets.id.get());
        }
    }

    //    public static interface IExtended {
    //        int op(int value);
    //
    //        default int addOne(int value) {
    //            return value + 1;
    //        }
    //    }
    //
    //    public static void printOp(int value, IExtended op) {
    //        System.out.println(op.op(value));
    //    }
    //
    //    public static void main(String[] args) {
    //        printOp(2, t -> { return addOne(t) + 1; });
    //    }

    //TODO need a better test database. Product, Sales, Purchases, Customer, Supplier perhaps.

    /*
     * we had
     * where(t -> t.id.is(2))
     * but this (a) means exposing is() to everyone, and (b) means we can't verify id is a column in our lambda context,
     * which is more problematic in a join:
     * join(..., j -> j.left.col1.is(j.right.col2)) because how do you adequately differentiate that from
     * join(..., j -> j.left.col1.is(t.col2)) - ie, you need the lambda context.
     * You could do
     * join(..., (q,j) -> q.is(j.left.col1, j.right.col2)) and reference the table from the column; this would
     * allow us to know whether a column was passed in to be referenced as a column or its current value.
     * Ideally we want to (a) not expose filter methods in the column class, and (b) use a safe context for the lambda.
     * It would be nice, lacking operator overloading to do:
     * (j -> is(j.left.col1, j.right.col2)) but I don't see how to get an 'is' method which is legal only in the
     * current lambda, without also exposing the filter context q.
     * So also... how do you make j.right.col2.get() illegal, in a lambda? Because it really should be.
     * It seems the column definitions should be a typed enumeration, and have little agency of their own.
     * You then have to call table.get(table.col2), and is(table.col2, 2) etc based on context.
     * if you have Context<Table>, then
     * where(c -> c.is(c.t.id, 2)) seems longwinded.
     */
    @Test public void testJoins() {
        //SingleQuery result = db.query("SELECT t.db_email, w.db_name FROM tables t INNER JOIN widgets w ON t.db_id = w.db_xtable WHERE t.db_id == ? ORDER BY w.db_id", 2);
        //TODO where clause
        ORM8Results<?> results = db.widgets.join(db.sprockets, (j) -> j.left.xtable.is(j.right.id)).where(j -> j.right.id.is(2)).order(j -> Order.asc(j.left.id)).select();
    }
    // oh dear. where f->f.field.is(f.field2) needs to discriminate between the value of field2, and the column.
    //    @Test public void testJoins() {
    //        //orderby
    //        //TODO SQLiteBuilder.toSQL -> if Param is a Column<?>, then it should build a query using the column, rather than a literal value.
    //        Results<V2<Table, Widget>> results = db.tables.join(db -> db.widgets, (table, widget) -> table.id.is(widget.xtable)).where((table, widget) -> table.id.is(2)).select();
    //        V2<Table, Widget> row = results.next();
    //        assertEquals("betty@test.com", row.a.email.get());
    //        assertEquals("button", row.b.name.get());
    //        row = results.next();
    //        assertEquals("betty@test.com", row.a.email.get());
    //        assertEquals("bar", row.b.name.get());
    //
    //        Results<Table> results = db.tables.join(db -> db.widgets, (table, widget) -> table.id.is(widget.xtable)).where((table, widget) -> table.id.is(2)).select((table, widget) -> table.all());
    //        Table row = results.next();
    //        assertEquals("betty@test.com", row.email.get());
    //        row = results.next();
    //        assertEquals("betty@test.com", row.email.get());
    //
    //        fail();
    //    }

    @Test public void testReadSingleRecords() {
        testInsertRecords();

        {
            // Get all records from the first record.
            Sprocket sprocket = db.sprockets.where((t) -> t.id.is(1)).first().select().get();
            assertEquals((Integer) 1, sprocket.id.get());
            assertEquals("bob@test.com", sprocket.email.get());
            assertEquals(3, db.sprockets.count());
        }

        {
        	//FIXME - Why bother doing this? We don't have a use case yet.
            // Get only the email address from the second record.
            Sprocket sprocket = db.sprockets.where((t) -> t.id.greaterThan(1)).first().select((t) -> t.only(t.email)).get();
            assertNull(sprocket.id.get()); // don't know what it is.
            assertEquals("betty@test.com", sprocket.email.get());
            assertEquals(3, db.sprockets.count());
        }
        {
            Sprocket table = db.sprockets.where((t) -> t.id.lessThan(2)).select((t) -> t.only(t.email)).get();
            assertNull(table.id.get()); // don't know what it is.
            assertEquals("bob@test.com", table.email.get());
            assertEquals(3, db.sprockets.count());
        }
        {
            assertEquals(3, db.sprockets.where((t) -> t.email.like("%test.com")).count());
            assertEquals(1, db.sprockets.where((t) -> t.email.like("%tty%")).count());
        }
    }

    @Test public void testReadMultipleRecords() {
        testInsertRecords();

        ORM8Results<Sprocket> sprockets = db.sprockets.all().select();
        assertEquals("bob@test.com", sprockets.get().email.get());
        assertEquals("betty@test.com", sprockets.get().email.get());
        assertEquals("bingo@test.com", sprockets.get().email.get());
        assertFalse(sprockets.isValid());
    }

    @Test public void testUpdateRecord() {
        testInsertRecords();

        {
            Sprocket sprocket = db.sprockets.where((t) -> t.id.is(1)).first().select().get();
            assertEquals((Integer) 1, sprocket.id.get());
            assertEquals("bob@test.com", sprocket.email.get());
            sprocket.email.set("bobby@test.com");
            sprocket.update();
        }

        {
            Sprocket sprocket = db.sprockets.where((t) -> t.id.is(1)).first().select().get();
            assertEquals((Integer) 1, sprocket.id.get());
            assertEquals("bobby@test.com", sprocket.email.get());
        }
    }

    @Test public void testDeleteSingleByQuery() {
        testInsertRecords();

        assertEquals(3, db.sprockets.count());

        // Delete with query
        db.sprockets.where((t) -> t.id.is(1)).delete();

        assertEquals(2, db.sprockets.count());
    }

    @Test public void testMinMax() {
        testInsertRecords();

        assertEquals(Integer.valueOf(1), db.sprockets.all().min((t) -> t.id));
        assertEquals(Integer.valueOf(3), db.sprockets.all().max((t) -> t.id));
    }

    @Test public void testDeleteCurrentRecord() {
        testInsertRecords();

        assertEquals(3, db.sprockets.count());

        // Delete with the current object
        Sprocket sprocket = db.sprockets.where((t) -> t.id.is(1)).first().select().get();
        sprocket.delete();

        assertEquals(2, db.sprockets.count());
    }

    @Test public void testDeleteAll() {
        testInsertRecords();

        assertEquals(3, db.sprockets.count());

        db.sprockets.all().delete();

        assertEquals(0, db.sprockets.count());
    }

    /**
     * If you want to do a join that returns an oddball output, or something
     * else hinky, you're on your own. But if you're returning columns that
     * match the current table, that, at least, is possible.
     */
    @Test public void testCustomSQL() {
        testInsertRecords();

        {
            Sprocket table = db.sprockets.select("SELECT * FROM tables WHERE db_id = 0").get();
            assertNull(table);
        }

        {
            // Get all records from the first record.
            Sprocket sprocket = db.sprockets.select("SELECT * FROM tables WHERE db_id = 1").get();
            assertEquals((Integer) 1, sprocket.id.get());
            assertEquals("bob@test.com", sprocket.email.get());
        }

        {
            // Get only the email address from the second record.
            Sprocket sprocket = db.sprockets.select("SELECT db_email FROM tables WHERE db_id > 1").get();
            assertEquals(null, sprocket.id.get()); // don't know what it is.
            assertEquals("betty@test.com", sprocket.email.get());
        }

    }

    static class MyDbConnection extends DbConnection<MyDbConnection> {
        final SprocketTable sprockets = new SprocketTable(this);
        final WidgetTable widgets = new WidgetTable(this);

        /** Use create */
        private MyDbConnection() {
        }

        static public MyDbConnection create(String connectionString) {
            return DbConnection.create(connectionString, () -> new MyDbConnection());
        }
    }

    public static class SprocketTable extends AbstractTable<SprocketTable, Sprocket, MyDbConnection> {
        public SprocketTable(MyDbConnection db) {
            super(db, "tables", Sprocket::new);
        }
    }

    public static class Sprocket extends AbstractSingleTableRecord<Sprocket> {
        private Sprocket(SprocketTable table) { //DB db) {
            super(table); //db);
        }

        public IntegerColumn id = fields.integerColumn("id", KeyType.AUTOINCREMENT);
        public StringColumn email = fields.stringColumn("email");
    }

    public static class WidgetTable extends AbstractTable<WidgetTable, Widget, MyDbConnection> {
        public WidgetTable(MyDbConnection db) {
            super(db, "widgets", Widget::new);
        }
    }

    public static class Widget extends AbstractSingleTableRecord<Widget> {
        protected Widget(WidgetTable table) {
            super(table);
        }

        public IntegerColumn id = fields.integerColumn("id", KeyType.AUTOINCREMENT);
        public IntegerColumn xtable = fields.integerColumn("xtable");
        public StringColumn name = fields.stringColumn("name", KeyType.NULLABLE);

    }
}
