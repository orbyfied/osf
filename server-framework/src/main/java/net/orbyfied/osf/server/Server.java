package net.orbyfied.osf.server;

import net.orbyfied.j8.event.ComplexEventBus;
import net.orbyfied.j8.event.EventListener;
import net.orbyfied.j8.event.util.Pipelines;
import net.orbyfied.osf.db.DatabaseManager;
import net.orbyfied.osf.network.NetworkManager;
import net.orbyfied.osf.network.handler.ChainAction;
import net.orbyfied.osf.network.handler.HandlerResult;
import net.orbyfied.osf.network.handler.NodeAction;
import net.orbyfied.osf.network.handler.UtilityNetworkHandler;
import net.orbyfied.osf.resource.ServerResourceManager;
import net.orbyfied.osf.server.common.GeneralProtocolSpec;
import net.orbyfied.osf.server.common.HandshakeProtocolSpec;
import net.orbyfied.osf.server.common.protocol.general.PacketUnboundDisconnect;
import net.orbyfied.osf.server.event.ServerClientConnectEvent;
import net.orbyfied.osf.server.event.ServerPostStartEvent;
import net.orbyfied.osf.server.event.ServerPrepareEvent;
import net.orbyfied.osf.server.event.ServerStopEvent;
import net.orbyfied.osf.server.exception.ClientConnectException;
import net.orbyfied.osf.server.exception.ServerInitializeException;
import net.orbyfied.osf.util.logging.EventLog;
import net.orbyfied.osf.util.logging.Logging;
import net.orbyfied.osf.util.Values;
import net.orbyfied.osf.util.Version;
import net.orbyfied.osf.util.security.AsymmetricEncryptionProfile;
import net.orbyfied.osf.util.worker.SafeWorker;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Abstract server template, obviously not
 * required but it does help by providing
 * a kind of preset with various features,
 * including encryption.
 *
 * Base server class.
 */
public abstract class Server
        /* registered to its own event bus */ implements EventListener {

    // server logger
    protected static final EventLog logger = Logging.getEventLog("Server");

    /*
        Special, internal keys.

        These are protected because they can't be
        specified in the configuration due to the
        fact they are unique objects and not strings.

        Often used for internal configuration, like
        enabling or disabling RSA encryption, etc.
     */

    /**
     * Internal key: Enable encryption
     */
    public static final Object K_ENABLE_ENCRYPTED = new Object();

    /**
     * Internal key: Enforce encryption
     */
    public static final Object K_ENFORCE_ENCRYPTED = new Object();

    /////////////////////////////////////////

    public Server(String name,
                  Version version) {
        // set fields
        this.name    = name;
        this.version = version;

        // initialize core services
        try {
            logger.info("init_core_services", "Initializing core services");

            this.networkManager  = new NetworkManager();
            this.databaseManager = new DatabaseManager();
            this.resourceManager = new ServerResourceManager(name)
                    .withDatabaseManager(databaseManager);
        } catch (Exception e) {
            throw new ServerInitializeException("Exception in instantiation", e);
        }

        // register to event bus
        eventBus.withDefaultPipelineFactory((bus, eventClass) -> Pipelines.mono(bus));
        eventBus.register(this);
    }

    // server properties
    private final String  name;
    private final Version version;

    public String getName() {
        return name;
    }

    public Version getVersion() {
        return version;
    }

    // server configuration
    protected final Values configuration = new Values();

    /*
        Core Services
    */

    // network manager, for handling networking
    protected final NetworkManager networkManager;

    // the database manager
    protected final DatabaseManager databaseManager;

    // the resource manager
    protected final ServerResourceManager resourceManager;

    public NetworkManager networkManager() {
        return networkManager;
    }

    public DatabaseManager databaseManager() {
        return databaseManager;
    }

    public ServerResourceManager resourceManager() {
        return resourceManager;
    }

    /*
        Networking
     */

    // the protocol specifications to load
    protected final List<ProtocolSpecification> protocolSpecifications = new ArrayList<>();
    // the server socket
    protected ServerSocket serverSocket;

    // top level utility network handler
    protected UtilityNetworkHandler utilityNetworkHandler;

    public UtilityNetworkHandler utilityNetworkHandler() {
        return utilityNetworkHandler;
    }

    /*
        Security
     */

    // top level RSA encryption profile
    protected AsymmetricEncryptionProfile rsaEncryptionProfile;

    /*
        Functional
     */

    // the client list instance factory
    protected Function<Server, ServerClientList> clientListFactory = ServerClientList::new;
    // the client instance factory
    protected BiFunction<Server, Socket, ServerClient> clientInstanceFactory = (server, socket) -> new ServerClient(this, socket);
    // the clients connected to this server
    private ServerClientList clients;

    // the event bus
    private final ComplexEventBus eventBus = new ComplexEventBus();

    public ComplexEventBus eventBus() {
        return eventBus;
    }

    // if it should be running
    private AtomicBoolean active;

    public Server setActive(boolean b) {
        this.active.set(b);
        return this;
    }

    // the server socket worker
    private SafeWorker serverSocketWorker;

    public SafeWorker serverSocketWorker() {
        return serverSocketWorker;
    }

    public ServerClientList clients() {
        return clients;
    }

    public Server stop() {
        // set inactive
        active.set(false);

        // call event
        eventBus.post(new ServerStopEvent(this));

        // return
        return this;
    }

    /**
     * Prepares the server to be opened
     * and used. This includes stuff like
     * connecting to databases.
     * @return This.
     */
    public Server prepare() {
        // call event
        eventBus.post(new ServerPrepareEvent(this));

        // set up resource manager
        resourceManager.setup();

        // create client list
        clients = clientListFactory.apply(this);

        // return
        return this;
    }

    /**
     * Bind and start the server on
     * the specified address.
     * @param address The address to bind the socket to.
     * @return This.
     */
    public Server open(SocketAddress address) {
        // set logger stage
        logger.stage("Connect");

        // load protocol
        protocolSpecifications.add(GeneralProtocolSpec.INSTANCE);
        if (configuration.getOrDefaultFlat(K_ENABLE_ENCRYPTED, true))
            protocolSpecifications.add(HandshakeProtocolSpec.INSTANCE);
        for (ProtocolSpecification spec : protocolSpecifications) {
            spec.apply(networkManager);
        }

        logger.newOk("load_protocol", "Loaded protocol consisting of " + protocolSpecifications.size()
                + " specifications").extra(v -> v.put("specs", protocolSpecifications)).push();

        // bind socket
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(address);

            logger.ok("server_socket_connect", "Connected server socket on {0}", address);
        } catch (Exception e) {
            logger.err("server_socket_connect", "Error while binding server socket to {0}", address);
            throw new ServerInitializeException("Connect: error while binding socket", e);
        }

        // create utility network handler
        utilityNetworkHandler = new UtilityNetworkHandler(networkManager, null)
                .owned(this);
        utilityNetworkHandler.start();

        // create encryption if wanted
        if (configuration.getOrDefaultFlat(K_ENABLE_ENCRYPTED, true)) {
            try {
                // create encryption profile
                rsaEncryptionProfile = GeneralProtocolSpec.newAsymmetricEncryptionProfile();

                // generate keys
                rsaEncryptionProfile.generateKeys();
            } catch (Exception e) {
                logger.err("rsa_encryption", e, "Failed to enable RSA encryption");
                throw new ServerInitializeException("Connect: error while enabling RSA encryption", e);
            }
        }

        // return
        logger.stage(null);
        return this;
    }

    /**
     * Starts the server processes.
     * @return This.
     */
    public Server start() {
        // post event
        eventBus.post(new ServerPostStartEvent(this));

        // create and run server socket worker
        logger.info("server_socket_worker", "Activating server socket");
        serverSocketWorker = new SafeWorker()
                .withErrorHandler((safeWorker, throwable) -> {
                    logger.err("server_socket_worker", "Error in server socket worker, shutting down");
                    throwable.printStackTrace();
                    stop();
                })
                .withTarget(this::mainServerSocketLoop)
                .withActivityPredicate(safeWorker -> active.get())
                .commence();

        // return
        return this;
    }

    // main server socket procedure
    private void mainServerSocketLoop() {
        // set thread local logger stage
        logger.stage("Socket");

        // while socket is not closed
        while (!serverSocket.isClosed()) {
            // accept connection
            Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
            } catch (Exception e) {
                // log error and continue
                logger.err("server_socket_accept", e, "Error while accepting connections");
                e.printStackTrace();
                continue;
            }

            // initialize and add client
            ServerClient client;
            try {
                // create client
                client = clientInstanceFactory.apply(this, clientSocket);

                // register and start client
                clients.add(client);
                client.start();

                // prepare disconnection
                client.networkHandler().node().childForType(PacketUnboundDisconnect.TYPE)
                        .<PacketUnboundDisconnect>withHandler((handler, node, packet) -> {
                            // send log
                            logger.info("client_disconnect", "Client {0} disconnected with message " +
                                    "'" + packet.getMessage() + "'", client);
                            // disconnect client
                            client.destroy();

                            // return
                            return new HandlerResult(ChainAction.CONTINUE).nodeAction(NodeAction.REMOVE);
                        });

                // prepare procedures
                if (configuration.getOrDefaultFlat(K_ENABLE_ENCRYPTED, true))
                    client.initSymmetricEncryptionHandshake();
                else
                    client.initRefuseSymmetricEncryptionHandshake();

                // call event
                eventBus.post(new ServerClientConnectEvent(this, client));
            } catch (ClientConnectException e) {
                if ("connect".equals(e.getMessage())) {
                    logger.err("client_connect", e,"Error connecting new client to socket {0}",
                            ServerClient.formatSocketAddress(clientSocket));
                } else {
                    logger.err("client_connect", e,"Unknown error while connecting new client to socket {0}",
                            ServerClient.formatSocketAddress(clientSocket));
                }
            } catch (Exception e) {
                // log error and continue
                logger.err("client_connect", e,"Error while accepting client from {0}",
                        ServerClient.formatSocketAddress(clientSocket));
                e.printStackTrace();
            }
        }
    }

}
