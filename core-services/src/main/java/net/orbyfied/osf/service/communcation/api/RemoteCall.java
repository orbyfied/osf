package net.orbyfied.osf.service.communcation.api;

import java.util.concurrent.CompletableFuture;

/**
 * An abstraction for futures when calling
 * a remote function.
 * @param <T> The return type.
 */
public class RemoteCall<T> {

    /**
     * The future which is completed when
     * sending is done.
     */
    CompletableFuture<Void> sendFuture;

    /**
     * The future which is completed when
     * a response to the call is received.
     */
    CompletableFuture<T> responseFuture;

    public CompletableFuture<Void> sent() {
        return sendFuture;
    }

    public CompletableFuture<T> response() {
        return responseFuture;
    }

}
