package net.orbyfied.msgs.client;

import net.orbyfied.msgs.common.Message;
import net.orbyfied.msgs.common.protocol.MessagePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public class MessageAPI {

    // the client
    private final MessageClient client;

    MessageAPI(MessageClient client) {
        this.client = client;
    }

    public MessageClient getClient() {
        return client;
    }

    /* ----------------------- */

    // the local handler map
    protected final Map<UUID, MessageHandler> localHandlers = new HashMap<>();

    public MessageHandler getHandler(UUID uuid) {
        return localHandlers.get(uuid);
    }

    public Map<UUID, MessageHandler> getLocalHandlers() {
        return localHandlers;
    }

    public MessageHandler on(MessageHandler handler) {
        // mark registered
        handler.register(this);

        // return
        return handler;
    }

    public MessageHandler on(String type, BiConsumer<MessageHandler, Message> action) {
        return on(new MessageHandler(type, action));
    }

    public MessageAPI send(Message message) {
        client.clientNetworkHandler().sendAsync(new MessagePacket(message));
        return this;
    }

}
