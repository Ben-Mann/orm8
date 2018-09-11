package net.benmann.orm8.db;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

public abstract class ColumnType<T> {
    final DataType dataType;

    private ColumnType(DataType dataType) {
        this.dataType = dataType;
    }

    /** Update the column with a value from a recordset. */
    abstract public void update(ResultSet rs, Column<T> column, int index) throws SQLException;

    /** Set a parameter in a prepared statement to this column's value. */
    abstract public void set(PreparedStatement ps, Column<T> column, int index) throws SQLException;

    /** Set a parameter in a prepared statement to a constant value. */
    abstract public void set(PreparedStatement ps, int index, T value) throws SQLException;

    /** Set this column to a value. */
    abstract public void setKey(Object o, Column<T> column) throws SQLException;

    /********************* Column Types *************************/

    //We store enums as integers, retrieved from an EnumeratedType
    public static <Q extends EnumeratedType> ColumnType<Q> createEnum(final ToEnumeratedTypeFn<Q> fn) {
        return new ColumnType<Q>(DataType.INTEGER) {

            @Override public void set(PreparedStatement ps, int index, Q value) throws SQLException {
                ps.setInt(index, value.getEnumeration());
            }

            @Override public void set(PreparedStatement ps, Column<Q> column, int index) throws SQLException {
                try {
                ps.setInt(index, column.get().getEnumeration());
                } catch (Throwable t) {
                    System.err.println(t.getMessage());
                }
            }

            @Override public void update(ResultSet rs, Column<Q> column, int index) throws SQLException {
                column.set(fn.f(index == -1 ? rs.getInt(column.getName()) : rs.getInt(index)));

            }

            @Override public void setKey(Object o, Column<Q> column) throws SQLException {
                column.set(fn.f((Integer) o));

            }

        };
    }
    
    public static final ColumnType<UUID> UUID = new ColumnType<UUID>(DataType.STRING) {

        @Override public void set(PreparedStatement ps, int index, java.util.UUID value) throws SQLException {
            ps.setString(index, value.toString());
        }

        @Override public void set(PreparedStatement ps, Column<java.util.UUID> column, int index) throws SQLException {
            ps.setString(index, column.get().toString());

        }

        @Override public void update(ResultSet rs, Column<java.util.UUID> column, int index) throws SQLException {
            column.set(java.util.UUID.fromString(index == -1 ? rs.getString(column.getName()) : rs.getString(index)));

        }

        @Override public void setKey(Object o, Column<java.util.UUID> column) throws SQLException {
            column.set(java.util.UUID.fromString((String) o));

        }
    };
    
    public static final ColumnType<Integer> INTEGER = new ColumnType<Integer>(DataType.INTEGER) {

        @Override public void set(PreparedStatement ps, int index, Integer value) throws SQLException {
			try {
				if (value == null) {
					ps.setNull(index, java.sql.Types.INTEGER);
					return;
				}
				ps.setInt(index, value);
			} catch (Throwable t) {
				throw new ORM8RuntimeException("Cannot set index " + index + " to '" + value + "'.");
			}
        }

        @Override public void set(PreparedStatement ps, Column<Integer> column, int index) throws SQLException {
            try {
            ps.setInt(index, column.get());
            } catch (Throwable t) {
                System.err.println(t.getMessage());
            }

        }

        @Override public void update(ResultSet rs, Column<Integer> column, int index) throws SQLException {
            column.set(index == -1 ? rs.getInt(column.getName()) : rs.getInt(index));

        }

        @Override public void setKey(Object o, Column<Integer> column) throws SQLException {
            column.set((Integer) o);
            
        }

    };

    public static final ColumnType<Double> DOUBLE = new ColumnType<Double>(DataType.DOUBLE) {
        @Override public void set(PreparedStatement ps, int index, Double value) throws SQLException {
            ps.setDouble(index, value);

        }

        @Override public void set(PreparedStatement ps, Column<Double> column, int index) throws SQLException {
            ps.setDouble(index, column.get());
        }

        @Override public void update(ResultSet rs, Column<Double> column, int index) throws SQLException {
            column.set(index == -1 ? rs.getDouble(column.getName()) : rs.getDouble(index));

        }

        @Override public void setKey(Object o, Column<Double> column) throws SQLException {
            column.set((Double) o);

        }

    };

    public static final ColumnType<double[]> DOUBLE_ARRAY = new ColumnType<double[]>(DataType.DOUBLE_ARRAY) {
        @Override public void set(PreparedStatement ps, int index, double[] value) throws SQLException {
            ps.setBytes(index, bytesFromDoubles(value));
        }

        @Override public void set(PreparedStatement ps, Column<double[]> column, int index) throws SQLException {
            ps.setBytes(index, bytesFromDoubles(column.get()));
        }

        @Override public void update(ResultSet rs, Column<double[]> column, int index) throws SQLException {
            column.set(doublesFromBytes(index == -1 ? rs.getBytes(column.getName()) : rs.getBytes(index)));

        }

        @Override public void setKey(Object o, Column<double[]> column) throws SQLException {
            column.set((double[]) o);
        }

        private double[] doublesFromBytes(byte[] bytes) {
            ByteBuffer bbuf = ByteBuffer.wrap(bytes);
            DoubleBuffer dbuf = bbuf.asDoubleBuffer();
            double[] result = new double[bytes.length / Double.BYTES];
            dbuf.get(result);
            return result;
        }

        private byte[] bytesFromDoubles(double[] doubles) {
            ByteBuffer bbuf = ByteBuffer.allocate(Double.BYTES * doubles.length);
            bbuf.asDoubleBuffer().put(doubles);
            return bbuf.array();
        }
    };

    public static final ColumnType<Long> LONG = new ColumnType<Long>(DataType.LONG) {

        @Override public void set(PreparedStatement ps, int index, Long value) throws SQLException {
            ps.setLong(index, value);

        }

        @Override public void set(PreparedStatement ps, Column<Long> column, int index) throws SQLException {
            ps.setLong(index, column.get());
        }

        @Override public void update(ResultSet rs, Column<Long> column, int index) throws SQLException {
            column.set(index == -1 ? rs.getLong(column.getName()) : rs.getLong(index));
            
        }

        @Override public void setKey(Object o, Column<Long> column) throws SQLException {
            column.set((Long) o);

        }

    };

    public static final ColumnType<Boolean> BOOLEAN = new ColumnType<Boolean>(DataType.BOOLEAN) {

        @Override public void set(PreparedStatement ps, int index, Boolean value) throws SQLException {
            ps.setBoolean(index, value);
        }

        @Override public void set(PreparedStatement ps, Column<Boolean> column, int index) throws SQLException {
            ps.setBoolean(index, column.get());
        }

        @Override public void update(ResultSet rs, Column<Boolean> column, int index) throws SQLException {
            column.set(index == -1 ? rs.getBoolean(column.getName()) : rs.getBoolean(index));

        }

        @Override public void setKey(Object o, Column<Boolean> column) throws SQLException {
            column.set((Boolean) o);

        }

    };

    public static final ColumnType<String> STRING = new ColumnType<String>(DataType.STRING) {

        @Override public void set(PreparedStatement ps, int index, String value) throws SQLException {
            ps.setString(index, value);

        }

        @Override public void set(PreparedStatement ps, Column<String> column, int index) throws SQLException {
            ps.setString(index, column.get());
        }

        @Override public void update(ResultSet rs, Column<String> column, int index) throws SQLException {
            column.set(index == -1 ? rs.getString(column.getName()) : rs.getString(index));

        }

        @Override public void setKey(Object o, Column<String> column) throws SQLException {
            column.set((String) o);
        }
        
    };

    //FIXME SQLiteBuilder should imlpement the column magic, because it doesn't have a date type, but other dbs do.
    public static final ColumnType<Date> DATE = new ColumnType<Date>(DataType.INTEGER) {

        @Override public void set(PreparedStatement ps, int index, Date value) throws SQLException {
            ps.setLong(index, value.getTime());
        }

        @Override public void set(PreparedStatement ps, Column<Date> column, int index) throws SQLException {
            ps.setLong(index, column.get().getTime());
        }

        @Override public void update(ResultSet rs, Column<Date> column, int index) throws SQLException {
            column.set(new Date(index == -1 ? rs.getLong(column.getName()) : rs.getLong(index)));
        }

        @Override public void setKey(Object o, Column<Date> column) throws SQLException {
            column.set((Date) o);
        }

    };
}