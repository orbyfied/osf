package net.orbyfied.osf.network;

import net.orbyfied.j8.registry.Identifier;

import java.io.DataInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PacketType<P extends Packet> {

    // the packet class
    final Class<P> type;

    // the identifier of this type
    final Identifier id;

    // serialization handlers
    Packets.Serializer<P>   serializer;
    Packets.Deserializer<P> deserializer;

    public PacketType(Class<P> type,
                      Identifier id) {
        this.type = type;
        this.id   = id;
    }

    public PacketType(Class<P> type,
                      String id) {
        this.type = type;
        this.id   = Identifier.of(id);
    }

    public Identifier identifier() {
        return id;
    }

    public Class<? extends Packet> getPacketClass() {
        return type;
    }

    public Packets.Serializer<P> serializer() {
        return serializer;
    }

    public PacketType<P> serializer(Packets.Serializer<P> serializer) {
        this.serializer = serializer;
        return this;
    }

    public Packets.Deserializer<P> deserializer() {
        return deserializer;
    }

    public PacketType<P> deserializer(Packets.Deserializer<P> deserializer) {
        this.deserializer = deserializer;
        return this;
    }

    public P deserialize(ObjectInputStream stream) throws Throwable {
        if (deserializer != null)
            return deserializer.deserialize(this, stream);
        return null;
    }

    public void serialize(P packet, ObjectOutputStream stream) throws Throwable {
        if (serializer != null)
            serializer.serialize(this, packet, stream);
    }

}
