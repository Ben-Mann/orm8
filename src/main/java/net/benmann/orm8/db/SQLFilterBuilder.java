package net.benmann.orm8.db;

import java.util.List;

import net.benmann.orm8.db.SQLiteBuilder.SQLBuilder;

public class SQLFilterBuilder {
    //    SQLFilterBuilder(StringBuilder sb, List<SQLFilterParam<?>> params) {
    //        tsql = sb.toString();
    //        this.params = params;
    //    }

    SQLFilterBuilder(SQLBuilder parts, List<SQLFilterParam<?>> params) {
        this.parts = parts;
        this.params = params;
    }

    //final String sql;
    final SQLBuilder parts;
    final List<SQLFilterParam<?>> params;

    @Override public String toString() {
        return parts.toString();
    }
}