package net.benmann.orm8.db;

@SuppressWarnings("serial")
public class ORM8RuntimeException extends RuntimeException {
    public ORM8RuntimeException(String message) {
        super(message);
    }

    public ORM8RuntimeException(String message, Throwable t) {
        super(message, t);
    }
}