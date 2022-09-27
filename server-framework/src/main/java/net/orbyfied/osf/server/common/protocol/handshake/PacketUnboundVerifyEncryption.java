package net.orbyfied.osf.server.common.protocol.handshake;

import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.PacketType;

public class PacketUnboundVerifyEncryption extends Packet {

    public static final PacketType<PacketUnboundVerifyEncryption> TYPE =
            new PacketType<>(PacketUnboundVerifyEncryption.class, "framework/handshake/unbound/verify")
            .serializer((type, packet, stream) -> stream.writeUTF(packet.message))
            .deserializer((type, stream) -> new PacketUnboundVerifyEncryption(stream.readUTF()));

    ////////////////////////////////////////////

    private String message;

    public PacketUnboundVerifyEncryption(String message) {
        super(TYPE);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
