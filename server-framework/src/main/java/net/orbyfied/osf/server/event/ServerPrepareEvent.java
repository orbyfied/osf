package net.orbyfied.osf.server.event;

import net.orbyfied.osf.server.Server;

/**
 * Event responsible for setting up the server.
 * This can be things like loading databases.
 *
 * Some things are expected, but not checked, to
 * be done here, like set the required primary
 * resource database in the resource manager.
 *
 * @see Server#prepare()
 */
public class ServerPrepareEvent extends ServerEvent {

    public ServerPrepareEvent(Server server) {
        super(server);
    }

}
