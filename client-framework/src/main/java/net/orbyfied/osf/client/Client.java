package net.orbyfied.osf.client;

import net.orbyfied.j8.event.ComplexEventBus;
import net.orbyfied.j8.event.EventListener;
import net.orbyfied.j8.event.util.Pipelines;
import net.orbyfied.osf.client.event.ClientConnectEvent;
import net.orbyfied.osf.client.event.ClientReadyEvent;
import net.orbyfied.osf.network.NetworkManager;
import net.orbyfied.osf.network.handler.ChainAction;
import net.orbyfied.osf.network.handler.HandlerResult;
import net.orbyfied.osf.network.handler.NodeAction;
import net.orbyfied.osf.network.handler.SocketNetworkHandler;
import net.orbyfied.osf.server.ProtocolSpecification;
import net.orbyfied.osf.server.ServerClient;
import net.orbyfied.osf.server.common.GeneralProtocolSpec;
import net.orbyfied.osf.server.common.HandshakeProtocolSpec;
import net.orbyfied.osf.server.common.protocol.general.PacketUnboundDisconnect;
import net.orbyfied.osf.server.common.protocol.handshake.PacketClientboundPubKey;
import net.orbyfied.osf.server.common.protocol.handshake.PacketServerboundClientKey;
import net.orbyfied.osf.server.common.protocol.handshake.PacketUnboundHandshakeStop;
import net.orbyfied.osf.server.event.ServerClientReadyEvent;
import net.orbyfied.osf.util.Values;
import net.orbyfied.osf.util.Version;
import net.orbyfied.osf.util.logging.EventLog;
import net.orbyfied.osf.util.logging.Logging;
import net.orbyfied.osf.util.security.AsymmetricEncryptionProfile;
import net.orbyfied.osf.util.security.SymmetricEncryptionProfile;

import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Client template, obviously not
 * required but it does help by providing
 * a kind of preset with various features,
 * including encryption.
 *
 * Base client class.
 */
public class Client
        /* registered to its own event bus */ implements EventListener {

    private static final EventLog LOGGER = Logging.getEventLog("Client");

    /*
        Internal Keys
     */

    public static final Object K_ENABLE_ENCRYPTED  = new Object();
    public static final Object K_ENFORCE_ENCRYPTED = new Object();

    /*
        General
     */

    private final String  name;
    private final Version version;

    /*
        Security
     */

    // the client symmetric encryption profile
    private final SymmetricEncryptionProfile clientEncryptionProfile =
            GeneralProtocolSpec.newSymmetricEncryptionProfile();
    // the server asymmetric encryption profile
    private final AsymmetricEncryptionProfile serverEncryptionProfile =
            GeneralProtocolSpec.newAsymmetricEncryptionProfile();

    /*
        Networking
     */

    // the jobs to execute before the connection is ready
    protected final HashSet<String> connectJobs = new HashSet<>();

    // the client network handler
    private final SocketNetworkHandler networkHandler;

    // the protocol specifications
    protected List<ProtocolSpecification> protocolSpecifications = new ArrayList<>();

    /*
        Services
     */

    // the main configuration
    protected final Values configuration = new Values();

    // the network manager
    private final NetworkManager networkManager = new NetworkManager();

    private final ComplexEventBus eventBus = (ComplexEventBus) new ComplexEventBus()
            .withDefaultPipelineFactory((bus, eventClass) -> Pipelines.mono(bus));

    public Client(String name, Version version) {
        this.name    = name;
        this.version = version;

        // create network handler
        networkHandler = networkManager.handler(
                new SocketNetworkHandler(networkManager, null)
        );
    }

    public ComplexEventBus eventBus() {
        return eventBus;
    }

    public SocketNetworkHandler clientNetworkHandler() {
        return networkHandler;
    }

    public NetworkManager networkManager() {
        return networkManager;
    }

    /*
        Loading
     */

    public void load() {
        // load protocol
        protocolSpecifications.add(GeneralProtocolSpec.INSTANCE);
        if (configuration.getOrDefaultFlat(K_ENABLE_ENCRYPTED, true))
            protocolSpecifications.add(HandshakeProtocolSpec.INSTANCE);
        for (ProtocolSpecification spec : protocolSpecifications)
            spec.apply(networkManager);
        LOGGER.newOk("load_protocol", "Loaded protocol consisting of " + protocolSpecifications.size() +
                " specifications.").extra(v -> v.put("specs", protocolSpecifications)).push();
    }

    /*
        Connection
     */

    public void disconnectWithMessage(String msg) {
        networkHandler.sendSync(new PacketUnboundDisconnect(msg));
        disconnect();
    }

    public void disconnect() {
        networkHandler.disconnect();
        networkHandler.stop();
    }

    protected void onConnectReady() {
        // log
        LOGGER.info("connect_ready", "Finished connection to " + ServerClient.formatSocketAddress(networkHandler.getSocket()));

        // push event
        eventBus.post(new ClientReadyEvent(this));
    }

    protected void pushConnectJob(String job) {
        connectJobs.add(job);
    }

    protected void finishConnectJob(String job) {
        int len;
        synchronized (connectJobs) {
            connectJobs.remove(job);
            len = connectJobs.size();
        }

        if (len == 0)
            onConnectReady();
    }

    private void preInitHandshake() {
        networkHandler.autoEncrypt(true);

        // wait for public key packet
        networkHandler.node().childForType(PacketClientboundPubKey.TYPE)
                .<PacketClientboundPubKey>withHandler((handler, node, packet) -> {
                    // set encryption
                    serverEncryptionProfile.withPublicKey(packet.getKey());

                    // generate secret key
                    // and send to server
                    clientEncryptionProfile.generateKeys();
                    networkHandler.sendSyncEncrypted(
                            new PacketServerboundClientKey(clientEncryptionProfile.getSecretKey()),
                            serverEncryptionProfile);
                    networkHandler.withEncryptionProfile(clientEncryptionProfile);

                    return new HandlerResult(ChainAction.CONTINUE).nodeAction(NodeAction.REMOVE);
                });

        // if handshake is stopped
        networkHandler.node().childForType(PacketUnboundHandshakeStop.TYPE)
                .<PacketUnboundHandshakeStop>withHandler((handler, node, packet) -> {
                    // check if enforced
                    if (configuration.getOrDefaultFlat(K_ENFORCE_ENCRYPTED, true)) {
                        disconnectWithMessage("Encryption required");
                    }

                    return new HandlerResult(ChainAction.CONTINUE).nodeAction(NodeAction.REMOVE);
                });
    }

    private void preInitRefuseHandshake() {
        // wait for public key packet
        networkHandler.node().childForType(PacketClientboundPubKey.TYPE)
                .<PacketClientboundPubKey>withHandler((handler, node, packet) -> {
                    // send back handshake stop packet
                    networkHandler.sendSync(new PacketUnboundHandshakeStop("Disabled"));
                    return new HandlerResult(ChainAction.CONTINUE).nodeAction(NodeAction.REMOVE);
                });
    }

    public void connect(Socket socket) {
        // log
        LOGGER.info("client_connect", "Connecting client to {0}",
                ServerClient.formatSocketAddress(socket));

        // disconnect from current server if needed
        if (networkHandler.isOpen()) {
            disconnect();
        }

        // initialize pre processes
        if (configuration.getOrDefaultFlat(K_ENABLE_ENCRYPTED, true))
            preInitHandshake();
        else
            preInitRefuseHandshake();

        // connect
        networkHandler.connect(socket);

        // push event
        eventBus.post(new ClientConnectEvent(this));
    }

    public void connect(SocketAddress address) {
        try {
            Socket socket = new Socket();
            socket.bind(address);
            connect(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
