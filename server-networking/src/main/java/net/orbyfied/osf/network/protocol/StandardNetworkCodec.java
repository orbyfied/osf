package net.orbyfied.osf.network.protocol;

import net.orbyfied.osf.network.NetworkManager;
import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.PacketType;
import net.orbyfied.osf.network.handler.SocketNetworkHandler;
import net.orbyfied.osf.util.security.EncryptionProfile;

import java.io.*;

/**
 * The standard network codec.
 * Format (One character is one byte):
 * - e = encrypted (boolean)
 * - i = hash of type id (int)
 * - l = data length (int) [only in encrypted mode]
 * - d = packet data (byte[])
 * Packet       {eiiii[llll]d+}
 * PacketStream {Packet...}
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class StandardNetworkCodec extends NetworkCodec {

    public StandardNetworkCodec() {
        super("standard");
    }

    @Override
    public void write(SocketNetworkHandler handler,
                      Protocol protocol,
                      Packet packet,
                      ObjectOutputStream outputStream,
                      boolean encrypt) throws Throwable {
        // get encryption
        EncryptionProfile encryption = handler.encryptionProfile();

        // get output stream
        ByteArrayOutputStream baos = null;
        ObjectOutputStream out;
        if (!encrypt) {
            out = outputStream;
        } else {
            // create encrypted output stream
            baos = new ByteArrayOutputStream();
            out  = encryption.encryptingOutputStream(baos).toObjectStream();
        }

        // write packet type
        outputStream.writeByte(/* unencrypted */ encrypt ? 1 : 0);
        outputStream.writeInt(packet.type().identifier().hashCode());

        // serialize packet
        packet.type().serialize(packet, out);
        out.flush();

        // write encrypted data if needed
        if (encrypt) {
            byte[] data = baos.toByteArray();
            outputStream.writeInt(data.length);
            outputStream.write(data);
        }

        // flush
        outputStream.flush();
    }

    @Override
    public Packet read(SocketNetworkHandler handler,
                       Protocol protocol,
                       ObjectInputStream inputStream) throws Throwable {
        // get manager
        NetworkManager manager = handler.manager();

        // listen for incoming packets
        byte encryptedFlag = inputStream.readByte();
        int packetTypeId   = inputStream.readInt();
        // get packet type
        PacketType<? extends Packet> packetType =
                manager.getByHash(packetTypeId);

        // handle packet
        if (packetType != null) {
            // prepare stream
            ObjectInputStream stream;
            if (encryptedFlag == 0) {
                // put unencrypted stream
                stream = inputStream;
            } else {
                // get decryption profile
                EncryptionProfile encryptionProfile = handler.encryptionProfile();

                // check for decryption profile
                if (encryptionProfile == null) {
                    throw new IllegalArgumentException("can not decrypt encrypted packet, no decryption profile set");
                }

                // read encrypted bytes
                int dataLen = inputStream.readInt();
                byte[] encrypted = inputStream.readNBytes(dataLen);

                // create encrypted input stream
                ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
                stream = new ObjectInputStream(encryptionProfile.decryptingInputStream(bais).toDataStream());
            }

            // deserialize
            Packet packet = packetType.deserialize(stream);

            // return
            return packet;
        }

        // was not a (valid) packet
        // return null
        return null;
    }

}
