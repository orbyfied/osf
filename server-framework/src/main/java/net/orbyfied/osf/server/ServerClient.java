package net.orbyfied.osf.server;

import net.orbyfied.osf.network.handler.ChainAction;
import net.orbyfied.osf.network.handler.HandlerResult;
import net.orbyfied.osf.network.handler.NodeAction;
import net.orbyfied.osf.network.handler.SocketNetworkHandler;
import net.orbyfied.osf.server.common.GeneralProtocolSpec;
import net.orbyfied.osf.server.common.protocol.general.PacketUnboundDisconnect;
import net.orbyfied.osf.server.common.protocol.handshake.PacketClientboundPubKey;
import net.orbyfied.osf.server.common.protocol.handshake.PacketServerboundClientKey;
import net.orbyfied.osf.server.common.protocol.handshake.PacketUnboundHandshakeStop;
import net.orbyfied.osf.server.common.protocol.handshake.PacketUnboundVerifyEncryption;
import net.orbyfied.osf.server.event.ServerClientReadyEvent;
import net.orbyfied.osf.server.exception.ClientConnectException;
import net.orbyfied.osf.util.logging.EventLog;
import net.orbyfied.osf.util.logging.Logging;
import net.orbyfied.osf.util.security.SymmetricEncryptionProfile;

import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

public class ServerClient {

    public static String formatSocketAddress(Socket socket) {
        return socket.getInetAddress() + ":" + socket.getPort();
    }

    protected static final EventLog log = Logging.getEventLog("ServerClient");

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
            clientSymmetricEncryption = GeneralProtocolSpec.newSymmetricEncryptionProfile();

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

    public ServerClient disconnectWithMessage(String message) {
        networkHandler.sendSync(new PacketUnboundDisconnect(message));
        networkHandler.disconnect();
        networkHandler.stop();
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
        server.eventBus().post(new ServerClientReadyEvent(server, this));
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

    // refuse the handshake by sending a stop packet immediately
    protected void initRefuseSymmetricEncryptionHandshake() {
        networkHandler.sendSync(new PacketUnboundHandshakeStop("Disabled"));
    }

    // starts the process to establish
    // an encrypted connection
    protected void initSymmetricEncryptionHandshake() {
        // post mandatory job
        postReadyJob("encryption-handshake");

        final AtomicReference<String> okMessage = new AtomicReference<>();

        // setup handler for client keys
        networkHandler.node().childForType(PacketServerboundClientKey.TYPE)
                .<PacketServerboundClientKey>withHandler((handler, node, packet) -> {
                    // set key and encryption profile
                    clientSymmetricEncryption.withKey("secret", packet.getKey());
                    networkHandler.withEncryptionProfile(clientSymmetricEncryption);

                    //

                    // remove node and return
                    return new HandlerResult(ChainAction.CONTINUE).nodeAction(NodeAction.REMOVE);
                });

        // handle verification
        networkHandler.node().childForType(PacketUnboundVerifyEncryption.TYPE)
                .<PacketUnboundVerifyEncryption>withHandler((handler, node, packet) -> {
                    // check message against sent
                    if (packet.getMessage().equals(okMessage.get() + /* modify a bit */ "-modified")) {
                        log.ok("verify_encryption", "Established and verified AES encrypted " +
                                "connection for {0}", this);

                        // finish encryption
                        finishReadyJob("encryption-handshake");
                    } else {
                        log.warn("verify_encryption", "AES verification failed for {0}", this);
                        if (server.configuration.getOrDefaultFlat(Server.K_ENFORCE_ENCRYPTED, true)) {
                            disconnectWithMessage("Encryption required");
                        } else {
                            // finish encryption
                            finishReadyJob("encryption-handshake");
                        }
                    }

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
