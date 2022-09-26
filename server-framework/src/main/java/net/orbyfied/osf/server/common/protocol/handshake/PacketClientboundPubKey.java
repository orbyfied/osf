package net.orbyfied.osf.server.common.protocol.handshake;

import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.PacketType;
import net.orbyfied.osf.server.Server;

import java.security.PublicKey;

public class PacketClientboundPubKey extends Packet {

    public static final PacketType<PacketClientboundPubKey> TYPE = new PacketType<>(PacketClientboundPubKey.class, "framework/handshake/clientbound/pubkey")
            .deserializer((type, stream) -> {
                String keyStr = stream.readUTF();
                return new PacketClientboundPubKey(Server.EP_ASYMMETRIC.decodeKeyFromBase64(PublicKey.class, keyStr));
            })
            .serializer((type, packet, stream) -> {
                stream.writeUTF(Server.EP_ASYMMETRIC.encodeKeyToBase64(packet.key));
            });

    /////////////////////////////////////////

    protected final PublicKey key;

    public PacketClientboundPubKey(PublicKey key) {
        super(TYPE);
        this.key = key;
    }

    public PublicKey getKey() {
        return key;
    }

}
