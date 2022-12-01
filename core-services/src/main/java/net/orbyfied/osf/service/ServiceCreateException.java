package net.orbyfied.osf.service;

public class ServiceCreateException extends RuntimeException {

    public ServiceCreateException(String message) {
        super(message);
    }

    public ServiceCreateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceCreateException(Throwable cause) {
        super(cause);
    }

    public ServiceCreateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
