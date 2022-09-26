package net.orbyfied.osf.server.common.protocol.handshake;

import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.PacketType;
import net.orbyfied.osf.server.Server;

import javax.crypto.SecretKey;

public class PacketServerboundClientKey extends Packet {

    public static final PacketType<PacketServerboundClientKey> TYPE = new PacketType<>(PacketServerboundClientKey.class, "framework/handshake/serverbound/clientkey")
            .deserializer((type, stream) -> {
                String keyStr = stream.readUTF();
                return new PacketServerboundClientKey(Server.EP_SYMMETRIC.decodeKeyFromBase64(SecretKey.class, keyStr));
            })
            .serializer((type, packet, stream) -> {
                stream.writeUTF(Server.EP_SYMMETRIC.encodeKeyToBase64(packet.key));
            });

    /////////////////////////////////////////

    protected final SecretKey key;

    public PacketServerboundClientKey(SecretKey key) {
        super(TYPE);
        this.key = key;
    }

    public SecretKey getKey() {
        return key;
    }
    
}
