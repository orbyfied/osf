package net.orbyfied.msgs.client;

import net.orbyfied.j8.util.functional.TriFunction;
import net.orbyfied.msgs.common.Message;
import net.orbyfied.msgs.common.protocol.MessageResponsePacket;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class MessageExchange {

    public interface ResponseHandler {

        Action handle(MessageExchange exchange, Message target, Message response);

    }

    public enum Action {
        /**
         * Close and end the message exchange, it
         * will not be able to receive any more
         * responses.
         */
        CLOSE,

        /**
         * Keep the message exchange.
         */
        KEEP
    }

    //////////////////////////////////////////////

    // the message api
    private final MessageAPI api;
    // the uuids of the sent messages
    private final Map<UUID, Message> sent;
    // the uuid of the last sent message
    private Message lastSent;

    // the response handler
    ResponseHandler responseHandler;

    MessageExchange(MessageAPI api) {
        this.api  = api;
        this.sent = new HashMap<>();
    }

    public Map<UUID, Message> getSent() {
        return sent;
    }

    public Message getLastSent() {
        return lastSent;
    }

    public ResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public MessageExchange setResponseHandler(ResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
        return this;
    }

    public MessageExchange send(Message message) {
        // add to exchange
        sent.put(message.getUUID(), message);
        lastSent = message;
        api.exchangeMap.put(message.getUUID(), this);

        // send message
        api.sendFree(message);

        // return
        return this;
    }

    public MessageExchange handleResponse(UUID targetUUID, Message message) {
        Message target = sent.get(targetUUID);
        Action action;
        if ((action = responseHandler.handle(this, target, message)) == Action.CLOSE) {
            close();
        }

        return this;
    }

    public MessageExchange close() {
        for (Map.Entry<UUID, Message> entry : sent.entrySet()) {
            api.exchangeMap.remove(entry.getKey());
        }

        return this;
    }

    public MessageAPI getAPI() {
        return api;
    }

}
