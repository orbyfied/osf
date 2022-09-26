package net.orbyfied.osf.server.event;

import net.orbyfied.j8.event.BusEvent;
import net.orbyfied.osf.server.Server;

public abstract class ServerEvent extends BusEvent {

    // the server the event happened in
    private final Server server;

    public ServerEvent(Server server) {
        this.server = server;
    }

    public Server server() {
        return server;
    }

}
