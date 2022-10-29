package net.orbyfied.msgs.common;

import net.orbyfied.osf.util.Values;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * A message object.
 */
public class Message {

    public static UUID newUUID() {
        return new UUID(
                System.currentTimeMillis(),
                System.nanoTime()
        );
    }

    //////////////////////////////////////////////////

    // the UUID of this message
    private final UUID uuid;

    // the type identifier
    private final int typeHash;
    // the values
    private Values values = new Values();

    public Message(UUID uuid, int typeHash) {
        this.uuid     = uuid;
        this.typeHash = typeHash;
    }

    public UUID getUUID() {
        return uuid;
    }

    public int getTypeHash() {
        return typeHash;
    }

    public Values values() {
        return values;
    }

    public Message values(Consumer<Values> consumer) {
        consumer.accept(values());
        return this;
    }

    public Message values(Values values) {
        this.values = values;
        return this;
    }

}
