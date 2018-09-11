package net.benmann.orm8.db;

@SuppressWarnings("serial")
public class DbException extends RuntimeException {
	DbException() {
		
	}
	
	DbException(String message) {
		super(message);
	}
	
	DbException(String message, Throwable cause) {
		super(message, cause);
	}
	
	DbException(Throwable cause) {
		super(cause);
	}
}