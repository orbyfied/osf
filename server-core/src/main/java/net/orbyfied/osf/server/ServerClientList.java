package net.orbyfied.osf.server;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerClientList {

    // the server
    private final Server server;

    public ServerClientList(Server server) {
        this.server = server;
    }

    public Server server() {
        return server;
    }

    /* ---- List ----- */

    // the client list
    protected final List<ServerClient> clients = new ArrayList<>();
    // the clients mapped by address
    protected final Map<SocketAddress, ServerClient> clientsByAddr = new HashMap<>();

    public ServerClientList add(ServerClient client) {
        clients.add(client);
        clientsByAddr.put(client.networkHandler().getSocket().getRemoteSocketAddress(), client);
        return this;
    }

    public ServerClientList remove(ServerClient client) {
        clients.remove(client);
        clientsByAddr.remove(client.networkHandler().getSocket().getRemoteSocketAddress(), client);
        return this;
    }

}
