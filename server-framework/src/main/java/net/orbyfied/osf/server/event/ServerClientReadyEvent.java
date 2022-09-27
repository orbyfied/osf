package net.orbyfied.osf.server.event;

import net.orbyfied.osf.server.Server;
import net.orbyfied.osf.server.ServerClient;

public class ServerClientReadyEvent extends ServerClientEvent {

    public ServerClientReadyEvent(Server server, ServerClient client) {
        super(server, client);
    }

}
