package net.benmann.orm8.db;

import java.util.ArrayList;
import java.util.List;

/**
 * FIXME - Eventually could support if()
 */
public class MigrationBuilder {
    final List<Migration> migrations = new ArrayList<Migration>();

    public MigrationBuilder add(Migration mig) {
        migrations.add(mig);
        return this;
    }

    public List<Migration> toList() {
        return migrations;
    }
}