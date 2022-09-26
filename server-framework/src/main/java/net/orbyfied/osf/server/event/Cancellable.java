package net.orbyfied.osf.server.event;

import java.util.HashSet;

/**
 * Weird implementation of a cancellable event
 * because we don't have multiple inheritance
 * or fields in interfaces so we've got to use
 * this weird implementation with a set.
 */
public interface Cancellable {

    HashSet<Cancellable> CANCELLED = new HashSet<>();

    ///////////////////////////////////////////

    default void cancel() {
        CANCELLED.add(this);
    }

    default void resume() {
        CANCELLED.remove(this);
    }

    default void cancel(boolean b) {
        if (b)
            cancel();
        else
            resume();
    }

    default boolean cancelled() {
        return CANCELLED.contains(this);
    }

}
