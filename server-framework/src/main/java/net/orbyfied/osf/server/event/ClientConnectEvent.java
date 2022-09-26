package net.orbyfied.osf.server.event;

import net.orbyfied.osf.server.Server;
import net.orbyfied.osf.server.ServerClient;

public class ClientConnectEvent extends ServerClientEvent {

    public ClientConnectEvent(Server server, ServerClient client) {
        super(server, client);
    }

}
