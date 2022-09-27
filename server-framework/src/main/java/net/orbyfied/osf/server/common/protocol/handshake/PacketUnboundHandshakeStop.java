package net.orbyfied.osf.server.common.protocol.handshake;

import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.PacketType;

public class PacketUnboundHandshakeStop extends Packet {

    public static final PacketType<PacketUnboundHandshakeStop> TYPE =
            new PacketType<>(PacketUnboundHandshakeStop.class, "framework/handshake/unbound/stop")
            .serializer((type, packet, stream) -> { stream.writeUTF(packet.message); })
            .deserializer((type, stream) -> new PacketUnboundHandshakeStop(stream.readUTF()));

    /////////////////////////////////////////////

    private String message;

    public PacketUnboundHandshakeStop(String message) {
        super(TYPE);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
