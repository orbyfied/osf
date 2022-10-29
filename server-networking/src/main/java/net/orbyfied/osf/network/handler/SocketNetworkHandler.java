package net.orbyfied.osf.network.handler;

import net.orbyfied.osf.network.NetworkHandler;
import net.orbyfied.osf.network.NetworkManager;
import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.PacketType;
import net.orbyfied.osf.network.protocol.Protocol;
import net.orbyfied.osf.util.security.EncryptionProfile;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Network handler for socket connections.
 * Bound to a socket will read, write and handle packets.
 */
@SuppressWarnings({"rawtypes"})
public class SocketNetworkHandler extends NetworkHandler<SocketNetworkHandler> {

    // async executor service
    static Executor executor = Executors.newFixedThreadPool(4);

    // the socket
    Socket socket;
    // the data streams
    DataInputStream  inputStream;
    DataOutputStream outputStream;
    ObjectInputStream  objectInput;
    ObjectOutputStream objectOutput;

    // disconnect handler
    Consumer<Throwable> disconnectHandler;

    // the protocol
    Protocol protocol;

    // decryption (and encryption) profile
    EncryptionProfile encryptionProfile;
    // if it should automatically write all packets encrypted
    boolean autoEncrypt;

    public SocketNetworkHandler(final NetworkManager manager,
                                final NetworkHandler parent) {
        super(manager, parent);
    }

    public SocketNetworkHandler withDisconnectHandler(Consumer<Throwable> consumer) {
        this.disconnectHandler = consumer;
        return this;
    }

    public synchronized SocketNetworkHandler withEncryptionProfile(EncryptionProfile profile) {
        this.encryptionProfile = profile;
        return this;
    }

    public synchronized SocketNetworkHandler withProtocol(Protocol protocol) {
        this.protocol = protocol;
        return this;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public EncryptionProfile encryptionProfile() {
        return encryptionProfile;
    }

    public synchronized SocketNetworkHandler autoEncrypt(boolean b) {
        this.autoEncrypt = b;
        return this;
    }

    @Override
    public SocketNetworkHandler fatalClose() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return this;
    }

    public SocketNetworkHandler connect(Socket socket) {
        this.socket = socket;

        try {
            inputStream  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            objectInput  = new ObjectInputStream(inputStream);
            objectOutput = new ObjectOutputStream(outputStream);
        } catch (Exception e) {
            fatalClose();
            LOGGER.err("socket_connect", "Error while connecting to " + socket.getRemoteSocketAddress());
            e.printStackTrace();
        }

        return this;
    }

    public SocketNetworkHandler disconnect() {
        // deactivate worker
        stop();

        // return
        return this;
    }

    public SocketNetworkHandler sendSyncRaw(Packet packet) {
        try {
            // call protocol write
            protocol.getEffectivePacketCodec().write(
                    this,
                    protocol,
                    packet,
                    objectOutput,
                    false
            );

            // return
            return this;
        } catch (Throwable t) {
            t.printStackTrace();
            return this;
        }
    }

    public CompletableFuture<SocketNetworkHandler> sendAsyncRaw(final Packet packet) {
        return CompletableFuture.supplyAsync(() -> {
            sendSyncRaw(packet);
            return this;
        }, executor);
    }

    public SocketNetworkHandler sendSync(Packet packet) {
        if (autoEncrypt && encryptionProfile != null) {
            return sendSyncEncrypted(packet, encryptionProfile);
        }

        return sendSyncRaw(packet);
    }

    public CompletableFuture<SocketNetworkHandler> sendAsync(final Packet packet) {
        return CompletableFuture.supplyAsync(() -> {
            sendSync(packet);
            return this;
        }, executor);
    }

    public SocketNetworkHandler sendSyncEncrypted(Packet packet, EncryptionProfile encryption) {
        try {
            // call protocol write
            protocol.getEffectivePacketCodec().write(
                    this,
                    protocol,
                    packet,
                    objectOutput,
                    true
            );

            // return
            return this;
        } catch (Throwable t) {
            t.printStackTrace();
            return this;
        }
    }

    public CompletableFuture<SocketNetworkHandler> sendAsyncEncrypted(final Packet packet,
                                                                      final EncryptionProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            sendSyncEncrypted(packet, profile);
            return this;
        }, executor);
    }

    public boolean isOpen() {
        if (socket == null)
            return false;
        return !socket.isClosed();
    }

    @Override
    protected WorkerThread createWorkerThread() {
        return new SocketWorkerThread();
    }

    public Socket getSocket() {
        return socket;
    }

    /* ---- Worker ---- */

    class SocketWorkerThread extends WorkerThread {

        @Override
        public void runSafe() throws Throwable {
            long pC = 0; // packet count
            Throwable t = null;

            // main network loop
            try {
                while (!socket.isClosed() && active.get()) {
                    // listen for incoming packets
                    Packet packet = protocol.getEffectivePacketCodec().read(
                            SocketNetworkHandler.this,
                            protocol,
                            objectInput
                    );

                    // check for packet
                    if (packet != null) {
                        // handle packet
                        SocketNetworkHandler.this.handle(packet);

                        // increment packet count
                        pC++;
                    }
                }
            } catch (Throwable t1) {
                t = t1;
            }

            // handle disconnect
            Throwable ft = null;
            if (t != null) {
                ft = t;
            }

            if (disconnectHandler != null)
                disconnectHandler.accept(ft);
        }
    }

}
