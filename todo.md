* automatic junction tables
- Junction(tablea, tableb, (a,b) -> a.field.is(b.field), ONE, MANY)
- int tablea:
-- Junction.hasMany(tableb)
-- Collection<tableb> hasMany(db -> db.tableb, (a,b) -> a.field.is(b.field))
- tableb.hasOne(tablea)

* should it not be code first? Maybe it can generate the script, but we expect the db to exist, to encourage a dba to create supporting indices etc?

* Concatenate select statements. So foo.where(condition).where(condition) is the same as foo.where(and(condition, condition)). As a result, you can have a field which provides a select on related records, and we 

* Create a view, like class Image { View<Dimension> dimensions = connection.dimensions.where(c -> c.image.is(productId.get())); ... then image.dimensions.create() will prepopulate the new Dimension.image with this image's productid, and any where() will be prepopulated with that query.

* Joins are very expensive to code, because a schema change requires modifying every single string.
* We need an Enum column type, because this turns out to be becoming a common pattern.

* You could change the select(c -> c.column.is(value)) notation to instead of using the row as a parameter, use
  something of type<row>, and then (t -> t.equals(t.column, value)) or ((t,r) -> t.equals(r.column, value))
  at which point you no longer need the is/gte/lt etc methods exposed to the column for all to see. 

* Make a class DataDB.Table<T> extends AbstractRow<T,DataDB>, and then subclass that.
* move AbstractTable instantiation to the table, don't do it in the db.
* move all sql into an sqlite class
* create migration from table columns and key
* table constructors shouldn't be necessary - the factory should do the creating for us.

**Migrations**
The AbstractRow record class will represent the class In This Version.
If there are no migrations defined, then this also defines the First Migration (CREATE TABLE).
If we change the record class, the generated First Migration won't match the one we save in the schema.
Therefore, we'll bail, saying that there's a missing migration step.
Designing the migration system and associated code is an exercise for a later date.

**Groups**
I want the max index from a table. How do I get it?
The current implementation doesn't work.
max(c->c.index) works, but all it's doing is identifying the column. So this doesn't work:
max(c->c.index.lt(5))
so the query needs to be ... different.

**Joins**
if Foo has columns id and name, like this:

```
IntegerColumn id = ..."id"
StringColumn name = ..."name"
```

There are several compound statements:

1. you have a list of Bars that link to Foo via an (implied) foreign key, you'd get the widgets normally with a query like this:

`SELECT * FROM bar WHERE foo_id = ?`

2. Or, all bars where Foo is called 'widget' like this:

`SELECT b.* FROM bar b WHERE foo_id IN (SELECT id FROM foo WHERE name='widget')`

3. Or, a list of foo and bar info for all foo starting with "wid"

`SELECT f.*, b.* FROM foo f INNER JOIN bar b ON b.foo_id = f.id WHERE f.name LIKE 'wid%'`

So how do we express these? Perhaps like this:

1. 

It probably should be a column, so that you can do other actions on it (and build compound queries)
`Column<List<Bar>> bars = id.map(connection.bars, (bars, id) -> bars.foo_id.is(id));`

2.

`Column<List<Bar>> widgets = id.map(connection.bars, (bars, id) -> bars.foo_id.is(id));`

3.

```
 Result<Pair<Foo,Bar>> getWidPairs() {
  return join(foo, bar, (f,b) -> f.id.eq(b.foo_id)).where((f,b) -> f.name.like("wid%"));
 }
```
