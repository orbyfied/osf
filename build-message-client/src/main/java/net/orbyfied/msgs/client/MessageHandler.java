package net.orbyfied.msgs.client;

import net.orbyfied.msgs.common.Message;

import java.util.UUID;
import java.util.function.BiConsumer;

public class MessageHandler {

    public static UUID newUUID() {
        return new UUID(
                System.currentTimeMillis(),
                System.nanoTime()
        );
    }

    ///////////////////////////////////////////////////

    // the message handler this is registered to
    private MessageAPI api;

    // the type to handle
    private final String type;
    private final int typeHash;

    // the uuid
    private final UUID uuid;

    // the action
    private final BiConsumer<MessageHandler, Message> action;

    public MessageHandler(String type, UUID uuid, BiConsumer<MessageHandler, Message> action) {
        this.type     = type;
        this.typeHash = type.hashCode();
        this.action   = action;
        this.uuid     = uuid;
    }

    public MessageHandler(String type, BiConsumer<MessageHandler, Message> action) {
        this(type, newUUID(), action);
    }

    public MessageHandler register(MessageAPI api) {
        this.api = api;
        return this;
    }

    public boolean isRegistered() {
        return api != null;
    }

    public MessageAPI getAPI() {
        return api;
    }

    public MessageHandler remove() {
        // remove from local
        api.localHandlers.remove(uuid);

        return this;
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getType() {
        return type;
    }

    public int getTypeHash() {
        return typeHash;
    }

}
