# A Simple SQL builder using Java8+ lambdas

This was built to experiment with a typesafe SQL builder / ORM which required neither reflection (it can be obfuscated) nor a precompiler. All queries are built dynamically. 

## Query Syntax

### Multiple Records (Recordset)

```java
ORM8Results<User> users = db.users.all().select();
//Get first user, automatically advance the recordset
User first = users.get();
```

### Single Record

Get first record where id == 1.

```java
User user = db.users.where((t) -> t.id.is(1)).first().select().get();
```

## Aggregates (Count etc)

Min id of all records (select min(*) from users)

```java
db.sprockets.all().min((t) -> t.id)
```

## Insert Syntax

Insert a new record

```java
User user = db.users.create();
user.email.set("user@example.com");
user.insert();
```

## Update Syntax

Set email for user with id 1

```java
User user = db.users.where((t) -> t.id.is(1)).first().select().get();
user.email.set("bobby@example.com");
user.update();
```

## Delete

### By Query
Delete where id == 1

```java
db.users.where((t) -> t.id.is(1)).delete();
```

### Current record

```java
User user = db.users.where((t) -> t.id.is(1)).first().select().get();
user.delete();
```

## Transactions

Typically, you use withTransaction:

```java
db.withTransaction(() -> {
    try {
        db.doSomething();
        db.doSomethingElse();
    } catch (ReasonToAbort e) {
        return Transaction.ROLLBACK;
    }
    return Transaction.COMMIT
});
```

or createTransaction's try-with-resources:

```java
try (Transaction t = db.createTransaction()) {
    db.doSomething();
    db.doSomethingElse();
    t.commit();
} catch (SillinessError e) {
    t.rollback();
}
```

## Arbitrary SQL

Assign to record object

```java
//Fill out entire record
User user = db.users.select("SELECT * FROM users WHERE db_id = 1").get();
//Only populate the email field - everything else will be null   
User user = db.users.select("SELECT db_email FROM users WHERE db_id = 1").get();
```

## Table Definition Syntax

Define a [users] table; will also generate the create DDL for you.

```java
public static class User extends AbstractSingleTableRecord<User> {
    public static class UserTable extends AbstractTable<UserTable, User, MyDbConnection> {
        public UserTable(MyDbConnection db) {
            super(db, "tables", User::new);
        }
    }

    private User(UserTable table) { //DB db) {
        super(table); //db);
    }

    public IntegerColumn id = fields.integerColumn("id", KeyType.AUTOINCREMENT);
    public StringColumn email = fields.stringColumn("email");
}
```

### Types

Custom Column types can be created and supported by the back end; built-in ColumnTypes include:

```
BooleanColumn
DateColumn
DoubleArrayColumn
DoubleColumn
IntegerColumn
UUIDColumn
EnumColumn<Q extends EnumeratedType>
LongColumn
StringColumn
```

## Database Connection

```java
static class MyDbConnection extends DbConnection<MyDbConnection> {
    final UserTable users = new UserTable(this);

    /** Use create */
    private MyDbConnection() {
    }

    static public MyDbConnection create(String connectionString) {
        return DbConnection.create(connectionString, () -> new MyDbConnection());
    }
}
```

## Database Support

This feature is not fully implemented.

All SQL is implemented in a builder class. See SQLiteBuilder for the SQLite implementation.

At present, DbConnection always instantiates the SQLiteBuilder back-end, however an additional parameter to DbConnection, or via the connection string, could allow switching SQL builders.

## Limitations

There are many. The primary one is that the SQL is built at runtime, which makes it very difficult to efficiently cache statements. A precompiling library may perform better in many circumstances.