package net.orbyfied.osf.server.event;

import net.orbyfied.osf.server.Server;
import net.orbyfied.osf.server.ServerClient;

public class ServerClientEvent extends ServerEvent {

    // the client involved in this event
    private final ServerClient client;

    public ServerClientEvent(Server server, ServerClient client) {
        super(server);
        this.client = client;
    }

    public ServerClient client() {
        return client;
    }

}
