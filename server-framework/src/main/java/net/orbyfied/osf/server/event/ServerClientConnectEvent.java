package net.orbyfied.osf.server.event;

import net.orbyfied.osf.server.Server;
import net.orbyfied.osf.server.ServerClient;

public class ServerClientConnectEvent extends ServerClientEvent {

    public ServerClientConnectEvent(Server server, ServerClient client) {
        super(server, client);
    }

}
