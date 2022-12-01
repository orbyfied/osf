package net.orbyfied.osf.network.packet;

import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.PacketType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class PacketCodec<P extends Packet> {

    // the parent codec
    // optional
    private final PacketCodec parent;

    // the type of packet
    private final PacketType type;

    public PacketCodec(PacketCodec parent,
                       PacketType packetType) {
        this.parent = parent;
        this.type   = packetType;
    }

    public PacketCodec parent() {
        return parent;
    }

    public PacketType packetType() {
        return type;
    }

    /*
        Methods for serialization and deserialization.
     */

    public void serialize(P packet, ObjectOutputStream stream) throws IOException {
        // serialize with this codec
        serialize0(packet, stream);
    }

    public P deserialize(ObjectInputStream stream) throws IOException {
        // deserialize with this codec
        return deserialize0(type, stream);
    }

    /*
        These are to be implemented when extending this class.
     */

    protected abstract void serialize0(P packet, ObjectOutputStream stream) throws IOException;

    protected abstract P deserialize0(PacketType type, ObjectInputStream stream) throws IOException;

}
