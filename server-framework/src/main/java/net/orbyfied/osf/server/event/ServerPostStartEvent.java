package net.orbyfied.osf.server.event;

import net.orbyfied.osf.server.Server;

public class ServerPostStartEvent extends ServerEvent {

    public ServerPostStartEvent(Server server) {
        super(server);
    }

}
