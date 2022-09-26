package net.orbyfied.osf.server.event;

import net.orbyfied.osf.server.Server;

public class ServerStopEvent extends ServerEvent {

    public ServerStopEvent(Server server) {
        super(server);
    }

}
