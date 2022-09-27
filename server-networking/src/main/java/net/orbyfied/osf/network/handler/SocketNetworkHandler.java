package net.orbyfied.osf.network.handler;

import net.orbyfied.osf.network.NetworkHandler;
import net.orbyfied.osf.network.NetworkManager;
import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.PacketType;
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
@SuppressWarnings({"unchecked", "rawtypes"})
public class SocketNetworkHandler extends NetworkHandler<SocketNetworkHandler> {

    // async executor service
    Executor executor = Executors.newSingleThreadExecutor();

    // the socket
    Socket socket;
    // the data streams
    DataInputStream  inputStream;
    DataOutputStream outputStream;

    // disconnect handler
    Consumer<Throwable> disconnectHandler;

    // decryption (and encryption) profile
    EncryptionProfile encryptionProfile;
    // if it should automatically write all packets encrypted
    boolean autoEncrypt;

    public SocketNetworkHandler(final NetworkManager manager,
                                final NetworkHandler parent) {
        super(manager, parent);
    }

    @Override
    protected void handle(Packet packet) {
        super.handle(packet);

        // call handler node
        this.node().handle(this, packet);
    }

    public SocketNetworkHandler withDisconnectHandler(Consumer<Throwable> consumer) {
        this.disconnectHandler = consumer;
        return this;
    }

    public synchronized SocketNetworkHandler withEncryptionProfile(EncryptionProfile profile) {
        this.encryptionProfile = profile;
        return this;
    }

    public synchronized SocketNetworkHandler autoEncrypt(boolean b) {
        this.autoEncrypt = b;
        return this;
    }

    @Override
    protected boolean canHandleAsync(Packet packet) {
        return false;
    }

    @Override
    protected void scheduleHandleAsync(Packet packet) {
        throw new UnsupportedOperationException();
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
            // write packet type
            outputStream.writeByte(/* unencrypted */ 0);
            outputStream.writeInt(packet.type().identifier().hashCode());

            // serialize packet
            packet.type().serializer().serialize(packet.type(), packet, outputStream);

            // flush
            outputStream.flush();

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
            // create output stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream stream    = encryption.encryptingOutputStream(baos).toDataStream();

            // serialize packet
            packet.type().serializer().serialize(packet.type(), packet, stream);
            stream.flush();

            // write packet type unencrypted
            outputStream.writeByte(/* encrypted */ 1);
            outputStream.writeInt(packet.type().identifier().hashCode());

            // get encrypted bytes and write
            byte[] encrypted = baos.toByteArray();
            outputStream.writeInt(encrypted.length); // write length
            outputStream.write(encrypted);

            // flush
            outputStream.flush();

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
                    byte encryptedFlag   = inputStream.readByte();
                    int packetTypeId = inputStream.readInt();
                    // get packet type
                    PacketType<? extends Packet> packetType =
                            manager.getByHash(packetTypeId);

//                    System.out.println("RECEIVED PACKET id-hash: " + packetTypeId + ", type: "
//                            + (packetType != null ? packetType.identifier().toString() : "null"));

                    // handle packet
                    if (packetType != null) {
                        // increment packet count
                        pC++;

                        // prepare stream
                        DataInputStream stream;
                        if (encryptedFlag == 0) {
                            // put unencrypted stream
                            stream = inputStream;
                        } else {
                            // check for decryption profile
                            if (encryptionProfile == null) {
                                throw new IllegalArgumentException("can not decrypt encrypted packet, no decryption profile set");
                            }

                            // read encrypted bytes
                            int dataLen = inputStream.readInt();
                            byte[] encrypted = inputStream.readNBytes(dataLen);

                            // create encrypted input stream
                            ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
                            stream = encryptionProfile.decryptingInputStream(bais).toDataStream();
                        }

                        // deserialize
                        Packet packet = packetType.deserializer()
                                .deserialize(packetType, stream);

                        // handle
                        SocketNetworkHandler.this.handle(packet);
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
