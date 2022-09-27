package net.orbyfied.osf.client;

import net.orbyfied.j8.event.ComplexEventBus;
import net.orbyfied.j8.event.EventListener;
import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.osf.network.NetworkManager;
import net.orbyfied.osf.network.handler.SocketNetworkHandler;
import net.orbyfied.osf.server.ProtocolSpecification;
import net.orbyfied.osf.server.common.GeneralProtocolSpec;
import net.orbyfied.osf.util.logging.Logging;
import net.orbyfied.osf.util.Version;
import net.orbyfied.osf.util.security.AsymmetricEncryptionProfile;
import net.orbyfied.osf.util.security.SymmetricEncryptionProfile;

import java.net.SocketAddress;
import java.util.List;

/**
 * Abstract client template, obviously not
 * required but it does help by providing
 * a kind of preset with various features,
 * including encryption.
 *
 * Base client class.
 */
public abstract class Client /* registered to its own event bus */
        implements EventListener {

    private static final Logger LOGGER = Logging.getLogger("Client");

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

    // the client network handler
    private final SocketNetworkHandler networkHandler;

    protected List<ProtocolSpecification> protocolSpecifications;

    /*
        Services
     */

    // the network manager
    private final NetworkManager networkManager = new NetworkManager();

    private final ComplexEventBus eventBus = new ComplexEventBus();

    public Client(String name, Version version) {
        this.name    = name;
        this.version = version;

        // create network handler
        networkHandler = new SocketNetworkHandler(networkManager, null);
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

    public Client load() {
        // load protocol

        // return
        return this;
    }

    public Client connect(SocketAddress address) {
        // disconnect from current server if needed
        if (networkHandler.isOpen()) {

        }

        // return
        return this;
    }

}
