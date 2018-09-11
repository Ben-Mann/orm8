package net.benmann.orm8.db;

public class SelectedColumns {
    final Aggregate<?> aggregate;
	final Column<?>[] columns;
	
	public SelectedColumns(Column<?>... columns) {
		this.columns = columns;
		aggregate = null;
	}
	
    public SelectedColumns(Aggregate<?> aggregate) {
		columns = null;
		this.aggregate = aggregate;
	}
}