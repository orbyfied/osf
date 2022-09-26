package net.orbyfied.osf.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Packets {

    public interface Serializer<P extends net.orbyfied.osf.network.Packet> {
        void serialize(net.orbyfied.osf.network.PacketType type, P packet,
                       DataOutputStream stream) throws Throwable;
    }

    public interface Deserializer<P extends net.orbyfied.osf.network.Packet> {
        P deserialize(net.orbyfied.osf.network.PacketType type, DataInputStream stream) throws Throwable;
    }

}
