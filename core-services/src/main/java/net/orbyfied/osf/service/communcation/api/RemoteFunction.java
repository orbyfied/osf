package net.orbyfied.osf.service.communcation.api;

/**
 * A function which is communicated to the
 * assigned remote when invoked.
 */
public interface RemoteFunction<T> {

    /**
     * Get the name of this function.
     */
    String getName();

    /**
     * If this function expects a response.
     */
    boolean expectsResponse();

    /**
     * The return type of this function.
     * Void if no response is expected.
     */
    Class<T> returnType();

    /**
     * Invokes the remote function.
     * @param args The arguments to send.
     * @return The call future.
     */
    RemoteCall<T> invoke(Object... args);

}
