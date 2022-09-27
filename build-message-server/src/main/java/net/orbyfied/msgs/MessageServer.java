package net.orbyfied.msgs;

import net.orbyfied.j8.event.handler.BasicHandler;
import net.orbyfied.osf.server.Server;
import net.orbyfied.osf.server.event.ServerClientReadyEvent;
import net.orbyfied.osf.server.event.ServerPrepareEvent;
import net.orbyfied.osf.util.Version;

public class MessageServer extends Server {

    public MessageServer(String name, Version version) {
        super("msgs", Version.of("0.1.0"));
    }

    /* -------------------------------------- */

    @BasicHandler
    void clientReady(ServerClientReadyEvent event) {

    }

    @BasicHandler
    void serverPrepare(ServerPrepareEvent event) {

    }

}
