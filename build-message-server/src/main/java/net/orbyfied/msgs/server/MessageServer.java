package net.orbyfied.msgs.server;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.orbyfied.j8.event.handler.BasicHandler;
import net.orbyfied.msgs.common.Message;
import net.orbyfied.msgs.common.protocol.MessageHandlerPacket;
import net.orbyfied.msgs.common.protocol.MessagePacket;
import net.orbyfied.msgs.common.protocol.MessageResponsePacket;
import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.handler.ChainAction;
import net.orbyfied.osf.network.handler.HandlerResult;
import net.orbyfied.osf.server.Server;
import net.orbyfied.osf.server.ServerClient;
import net.orbyfied.osf.server.event.ServerClientReadyEvent;
import net.orbyfied.osf.server.event.ServerPrepareEvent;
import net.orbyfied.osf.util.Version;

import java.util.*;

public class MessageServer extends Server {

    public MessageServer(String name, Version version) {
        super("msgs", Version.of("0.1.0"));
    }

    /* -------------------------------------- */

    // all handlers registered
    private final Map<UUID, MessageHandlerInfo> allHandlers = new HashMap<>();
    // handlers by client
    private final Map<ServerClient, List<MessageHandlerInfo>> clientHandlers = new HashMap<>();
    // clients by type they listen to
    private final Int2ObjectOpenHashMap<List<MessageHandlerInfo>> handlerTypes = new Int2ObjectOpenHashMap<>();

    public void addOne(MessageHandlerInfo handlerInfo) {
        allHandlers.put(handlerInfo.uuid(), handlerInfo);
        handlerTypes.computeIfAbsent(handlerInfo.typeHash(), __ -> new ArrayList<>()).add(handlerInfo);
        clientHandlers.computeIfAbsent(handlerInfo.client(), __ -> new ArrayList<>()).add(handlerInfo);
    }

    public void removeOne(MessageHandlerInfo handlerInfo) {
        allHandlers.remove(handlerInfo.uuid());
        List<MessageHandlerInfo> chl;
        if ((chl = clientHandlers.get(handlerInfo.client())) != null) {
            chl.remove(handlerInfo);
        }
    }

    public void removeAllOf(ServerClient client) {
        List<MessageHandlerInfo> handlers;
        if ((handlers = clientHandlers.get(client)) == null)
            return;
        clientHandlers.remove(client);
        for (MessageHandlerInfo handler : handlers) {
            removeOne(handler);
        }
    }

    public void handleResponse(UUID targetUUID, Message message) {
        // broadcast
        MessageResponsePacket packet = new MessageResponsePacket(targetUUID, message);
        for (ServerClient client : clients().list()) {
            client.networkHandler().sendSync(packet);
        }
    }

    public void handle(Message message) {
        MessagePacket packet = new MessagePacket(message);
        for (ServerClient client : clients().list()) {
            client.networkHandler().sendSync(packet);
        }

//        MessagePacket packet = new MessagePacket(message);
//        int type = message.getTypeHash();
//        List<MessageHandlerInfo> handlers = handlerTypes.get(type);
//        if (handlers == null)
//            return;
//        HashSet<ServerClient> called = new HashSet<>();
//        for (MessageHandlerInfo handler : handlers) {
//            ServerClient client = handler.client();
//            if (!called.contains(client)) {
//                called.add(client);
//                client.networkHandler().sendSync(packet);
//            }
//        }
    }

    /* -------------------------------------- */

    @BasicHandler
    void clientReady(ServerClientReadyEvent event) {
        final ServerClient client = event.client();

        client.networkHandler().withDisconnectHandler(throwable -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }

            removeAllOf(client);
        });

        // listen for packets
        client.networkHandler().node().childForType(MessageHandlerPacket.TYPE)
                .<MessageHandlerPacket>withHandler((handler, node, packet) -> {
                    MessageHandlerPacket.Action action = packet.getAction();
                    if (action == MessageHandlerPacket.Action.REMOVE_ALL) {
                        // remove all handlers of client
                        removeAllOf(client);
                    } else if (action == MessageHandlerPacket.Action.REMOVE_1) {
                        // remove one handler
                        MessageHandlerInfo handlerInfo = allHandlers.get(packet.getHandlerUUID());
                        if (handlerInfo.client() == client) {
                            removeOne(handlerInfo);
                        }
                    } else if (action == MessageHandlerPacket.Action.ADD_1) {
                        // add one handler
                        MessageHandlerInfo handlerInfo = new MessageHandlerInfo(
                                packet.getHandlerUUID(), client, packet.getTypeHash()
                        );
                        addOne(handlerInfo);
                    }

                    return new HandlerResult(ChainAction.CONTINUE);
                });

        client.networkHandler().node().childForType(MessagePacket.TYPE)
                .<MessagePacket>withHandler((handler, node, packet) -> {
                    handle(packet.getMessage());
                    return new HandlerResult(ChainAction.CONTINUE);
                });

        client.networkHandler().node().childForType(MessageResponsePacket.TYPE)
                .<MessageResponsePacket>withHandler((handler, node, packet) -> {
                    handleResponse(packet.getTargetUUID(), packet.getMessage());
                    return new HandlerResult(ChainAction.CONTINUE);
                });
    }

    @BasicHandler
    void serverPrepare(ServerPrepareEvent event) {

    }

}
