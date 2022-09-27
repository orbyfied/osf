package net.orbyfied.msgs.common;

import net.orbyfied.osf.util.Values;

import java.util.function.Consumer;

/**
 * A message object.
 */
public class Message {

    // the type identifier
    private final int typeHash;
    // the values
    private Values values = new Values();

    public Message(int typeHash) {
        this.typeHash = typeHash;
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
