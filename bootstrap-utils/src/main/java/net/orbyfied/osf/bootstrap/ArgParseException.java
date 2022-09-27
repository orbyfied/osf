package net.orbyfied.osf.bootstrap;

public class ArgParseException extends RuntimeException {

    public ArgParseException(String message) {
        super(message);
    }

    public ArgParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArgParseException(Throwable cause) {
        super(cause);
    }

}
