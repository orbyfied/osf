package net.orbyfied.osf.network.protocol;

public class Protocol {

    /**
     * The standard protocol specification.
     */
    public static final Protocol STANDARD = new Protocol(null, new StandardNetworkCodec());

    ///////////////////////////////////////////////

    // the parent protocol
    final Protocol parent;

    // the packet codec this protocol specifies
    final NetworkCodec packetCodec;
    // the packet codec to be used
    NetworkCodec effectivePacketCodec;

    public Protocol(Protocol parent,
                    NetworkCodec packetCodec) {
        // set parent
        this.parent = parent;

        // set packet codec
        this.packetCodec = packetCodec;

        // get effective packet codec
        this.effectivePacketCodec = calculateEffectivePacketCodec();
    }

    // recalculates the effective packet codec
    NetworkCodec calculateEffectivePacketCodec() {
        if (this.packetCodec != null) {
            return this.packetCodec;
        }

        // check for parent
        if (parent == null) {
            throw new IllegalStateException("Protocol does not have a packet codec");
        }

        // call parent
        return parent.calculateEffectivePacketCodec();
    }

    public Protocol getParent() {
        return parent;
    }

    public NetworkCodec getPacketCodec() {
        return packetCodec;
    }

    public NetworkCodec getEffectivePacketCodec() {
        return effectivePacketCodec;
    }

}
