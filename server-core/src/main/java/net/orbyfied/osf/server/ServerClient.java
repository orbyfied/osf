package net.orbyfied.osf.server;

import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.osf.network.handler.SocketNetworkHandler;
import net.orbyfied.osf.server.exception.ClientConnectException;
import net.orbyfied.osf.util.Logging;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerClient {

    public static String formatSocketAddress(Socket socket) {
        return socket.getInetAddress() + ":" + socket.getPort();
    }

    protected static final Logger logger = Logging.getLogger("ServerClient");

    ///////////////////////////////////////////////

    // the server this client is on
    private final Server server;

    // the client network handler
    private final SocketNetworkHandler networkHandler;

    // is active
    private boolean active;

    public ServerClient(Server server,
                        Socket socket) {
        // register server
        this.server = server;

        // create and initialize network handler
        this.networkHandler = new SocketNetworkHandler(server.networkManager(),
                server.utilityNetworkHandler());

        // connect network handler
        try {
            networkHandler.connect(socket);
        } catch (Exception e) {
            throw new ClientConnectException("connect", e);
        }
    }

    public SocketNetworkHandler networkHandler() {
        return networkHandler;
    }

    public Server server() {
        return server;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Start the client processes, like the
     * network handler.
     * @return This.
     */
    public ServerClient start() {
        // start network handler
        networkHandler.start();

        // return
        return this;
    }

    /**
     * Stop the client processes, like
     * the network handler.
     * @return This.
     */
    public ServerClient stop() {
        // stop network handler
        networkHandler.stop();
        networkHandler.disconnect();

        // return
        return this;
    }

    /**
     * Remove and destroy this client.
     * @return This.
     */
    public ServerClient destroy() {
        // check active
        // if so stop
        if (isActive()) {
            stop();
        }

        // remove client from server
        server.clients().remove(this);

        // return
        return this;
    }

    ///////////////////////////////

    @Override
    public String toString() {
        String str = "ServerClient[";
        if (networkHandler != null && networkHandler.isOpen())
            str += formatSocketAddress(networkHandler.getSocket());
        return str + "]";
    }

}
