package net.orbyfied.osf.server;

import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.osf.network.handler.ChainAction;
import net.orbyfied.osf.network.handler.HandlerResult;
import net.orbyfied.osf.network.handler.NodeAction;
import net.orbyfied.osf.network.handler.SocketNetworkHandler;
import net.orbyfied.osf.server.common.protocol.handshake.PacketClientboundPubKey;
import net.orbyfied.osf.server.common.protocol.handshake.PacketServerboundClientKey;
import net.orbyfied.osf.server.event.ClientReadyEvent;
import net.orbyfied.osf.server.exception.ClientConnectException;
import net.orbyfied.osf.util.Logging;
import net.orbyfied.osf.util.security.SymmetricEncryptionProfile;

import java.net.Socket;
import java.util.HashSet;

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

    // the jobs to undergo until ready
    private final HashSet<String> readyJobs = new HashSet<>();

    public ServerClient(Server server,
                        Socket socket) {
        // register server
        this.server = server;

        // create and initialize network handler
        boolean encrypt = server.configuration.getOrDefaultFlat(Server.K_ENABLE_ENCRYPTED, true);
        this.networkHandler = new SocketNetworkHandler(server.networkManager(),
                server.utilityNetworkHandler())
                .autoEncrypt(encrypt);

        if (encrypt)
            clientSymmetricEncryption = Server.newSymmetricEncryptionProfile();

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
        // disconnect and stop network handler
        if (networkHandler.isOpen())
            networkHandler.disconnect();
        if (networkHandler.active())
            networkHandler.stop();

        // return
        return this;
    }

    /**
     * Remove and destroy this client.
     * @return This.
     */
    public ServerClient destroy() {
        // check open
        // if so disconnect
        if (networkHandler.isOpen()) {
            networkHandler.disconnect();
        }

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

    // called when the client should
    // be marked ready
    protected void readyClient() {
        // call event
        server.eventBus().post(new ClientReadyEvent(server, this));
    }

    // called when a job is completed
    protected void finishReadyJob(String job) {
        int len;
        synchronized (readyJobs) {
            // remove job
            readyJobs.remove(job);

            // save length
            len = readyJobs.size();
        }

        // check if ready
        if (len == 0) {
            // ready client
            // TODO: do sync
            readyClient();
        }
    }

    // push a new mandatory job which
    // has to be completed for the client
    // to be ready
    protected void postReadyJob(String job) {
        synchronized (readyJobs) {
            readyJobs.add(job);
        }
    }

    /* ---- Encryption ---- */

    protected SymmetricEncryptionProfile clientSymmetricEncryption;

    public SymmetricEncryptionProfile getClientSymmetricEncryption() {
        return clientSymmetricEncryption;
    }

    // starts the process to establish
    // an encrypted connection
    protected void initSymmetricEncryptionHandshake() {
        // post mandatory job
        postReadyJob("encryption-handshake");

        // setup handler for client keys
        networkHandler.node().childForType(PacketServerboundClientKey.TYPE)
                .<PacketServerboundClientKey>withHandler((handler, node, packet) -> {
                    // set key and encryption profile
                    clientSymmetricEncryption.withKey("secret", packet.getKey());
                    networkHandler.withEncryptionProfile(clientSymmetricEncryption);
                    logger.ok("Established encrypted connection with {0}", this);

                    // finish job
                    finishReadyJob("encryption-handshake");

                    // remove node and return
                    return new HandlerResult(ChainAction.CONTINUE).nodeAction(NodeAction.REMOVE);
                });

        // send public key packet
        networkHandler
                .withEncryptionProfile(server.rsaEncryptionProfile)
                .sendSync(new PacketClientboundPubKey(server.rsaEncryptionProfile.getPublicKey()));
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
