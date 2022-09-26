package net.orbyfied.osf.server.exception;

public class ServerInitializeException extends RuntimeException {

    public ServerInitializeException() { }

    public ServerInitializeException(String message) {
        super(message);
    }

    public ServerInitializeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerInitializeException(Throwable cause) {
        super(cause);
    }

}
