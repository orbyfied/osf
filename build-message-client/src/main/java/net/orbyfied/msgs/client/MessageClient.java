package net.orbyfied.msgs.client;

import net.orbyfied.j8.event.handler.BasicHandler;
import net.orbyfied.msgs.common.Message;
import net.orbyfied.msgs.common.protocol.MessagePacket;
import net.orbyfied.msgs.common.protocol.MessageResponsePacket;
import net.orbyfied.osf.client.Client;
import net.orbyfied.osf.client.event.ClientReadyEvent;
import net.orbyfied.osf.network.handler.ChainAction;
import net.orbyfied.osf.network.handler.HandlerResult;
import net.orbyfied.osf.util.Version;

import javax.swing.*;
import java.util.UUID;

public class MessageClient extends Client {

    public MessageClient() {
        super("msgs", Version.of("0.1.0"));
        // set configurations
        configuration.put(K_ENABLE_ENCRYPTED,  true);
        configuration.put(K_ENFORCE_ENCRYPTED, true);
    }

    /*
        Client API
     */

    // the message api
    private MessageAPI api;

    public MessageAPI getAPI() {
        return api;
    }

    /*
        Client Implementation
     */

    @BasicHandler
    void clientReady(ClientReadyEvent event) {
        clientNetworkHandler().node().childForType(MessagePacket.TYPE)
                .<MessagePacket>withHandler((handler, node, packet) -> {
                    Message message = packet.getMessage();
                    api.handle(message);
                    return new HandlerResult(ChainAction.CONTINUE);
                });

        clientNetworkHandler().node().childForType(MessageResponsePacket.TYPE)
                .<MessageResponsePacket>withHandler((handler, node, packet) -> {
                    UUID targetUuid  = packet.getTargetUUID();
                    Message response = packet.getMessage();
                    api.handleResponse(targetUuid, response);
                    return new HandlerResult(ChainAction.CONTINUE);
                });
    }

}
