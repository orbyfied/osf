package net.orbyfied.osf.bootstrap;

public class ServerBootstrapException extends RuntimeException {

    public ServerBootstrapException() { }

    public ServerBootstrapException(String message) {
        super(message);
    }

    public ServerBootstrapException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerBootstrapException(Throwable cause) {
        super(cause);
    }

}
