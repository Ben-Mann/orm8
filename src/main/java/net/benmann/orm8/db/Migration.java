package net.benmann.orm8.db;

public abstract class Migration {
    abstract protected void up(DbConnection<?> db);

    abstract protected void down(DbConnection<?> db);
}