package net.orbyfied.osf.network;

@SuppressWarnings("rawtypes")
public abstract class Packet {

    // the packet type
    final net.orbyfied.osf.network.PacketType<? extends Packet> type;

    public Packet(net.orbyfied.osf.network.PacketType<? extends Packet> type) {
        this.type = type;
    }

    public net.orbyfied.osf.network.PacketType type() {
        return type;
    }

}
