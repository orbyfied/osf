package net.orbyfied.osf.server.common.protocol.general;

import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.PacketType;

public class PacketUnboundDisconnect extends Packet {

    public static final PacketType<PacketUnboundDisconnect> TYPE =
            new PacketType<>(PacketUnboundDisconnect.class, "framework/general/unbound/disconnect")
            .serializer((type, packet, stream) -> stream.writeUTF(packet.message))
            .deserializer((type, stream) -> new PacketUnboundDisconnect(stream.readUTF()));

    /////////////////////////////////

    String message;

    public PacketUnboundDisconnect(String message) {
        super(TYPE);
    }

    public String getMessage() {
        return message;
    }

}
