package net.orbyfied.msgs.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.orbyfied.msgs.common.Message;
import net.orbyfied.msgs.common.protocol.MessageHandlerPacket;
import net.orbyfied.msgs.common.protocol.MessagePacket;
import net.orbyfied.msgs.server.MessageHandlerInfo;

import java.util.*;
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
    // the types this API is registered to
    protected final Int2ObjectOpenHashMap<List<MessageHandler>> typeMap = new Int2ObjectOpenHashMap<>();

    // open exchanges
    protected final Map<UUID, MessageExchange> exchangeMap = new HashMap<>();

    public void handle(Message message) {
        List<MessageHandler> list = typeMap.get(message.getTypeHash());
        if (list == null)
            return;
        for (MessageHandler handler : list)
            handler.getAction().accept(handler, message);
    }

    public void handleResponse(UUID target, Message message) {
        MessageExchange exchange = exchangeMap.get(target);
        if (exchange == null)
            return;
        exchange.handleResponse(target, message);
        handle(message);
    }

    public MessageHandler get(UUID uuid) {
        return localHandlers.get(uuid);
    }

    public Map<UUID, MessageHandler> all() {
        return localHandlers;
    }

    public List<MessageHandler> onType(int typeHash) {
        return typeMap.get(typeHash);
    }

    public MessageAPI close() {
        // remove all handlers
        client.clientNetworkHandler().sendSync(new MessageHandlerPacket(MessageHandlerPacket.Action.REMOVE_ALL));

        // return
        return this;
    }

    public MessageAPI remove(MessageHandler handler) {
        return remove(handler.getUUID());
    }

    public MessageAPI remove(UUID handlerUUID) {
        // unregister from server
        client.clientNetworkHandler().sendSync(new MessageHandlerPacket(MessageHandlerPacket.Action.REMOVE_1)
                .setHandlerUUID(handlerUUID));

        // unregister locally
        MessageHandler handler = localHandlers.remove(handlerUUID);
        typeMap.get(handler.getTypeHash()).remove(handler);

        // return
        return this;
    }

    public MessageHandler listen(MessageHandler handler) {
        // register to server
        client.clientNetworkHandler().sendSync(new MessageHandlerPacket(MessageHandlerPacket.Action.ADD_1)
                .setTypeHash(handler.getTypeHash())
                .setHandlerUUID(handler.getUUID()));


        // mark registered
        handler.register(this);
        localHandlers.put(handler.getUUID(), handler);
        typeMap.computeIfAbsent(handler.getTypeHash(), __ -> new ArrayList<>()).add(handler);

        // return
        return handler;
    }

    public MessageHandler listen(String type, BiConsumer<MessageHandler, Message> action) {
        return listen(new MessageHandler(type, action));
    }

    public Message create(String type) {
        return new Message(Message.newUUID(), type.hashCode());
    }

    public MessageAPI sendFree(Message message) {
        client.clientNetworkHandler().sendSync(new MessagePacket(message));
        return this;
    }

    public MessageExchange send(Message message) {
        MessageExchange exchange = new MessageExchange(this);
        return exchange.send(message);
    }

}
