package net.orbyfied.osf.server.exception;

public class ClientConnectException extends RuntimeException {

    public ClientConnectException() { }

    public ClientConnectException(String message) {
        super(message);
    }

    public ClientConnectException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientConnectException(Throwable cause) {
        super(cause);
    }

}
