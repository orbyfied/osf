package net.orbyfied.osf.network.protocol;

import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.handler.SocketNetworkHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * The codec for packets as a whole, potentially
 * including things like packet ID, data, encryption, etc.
 * This is global to the protocol and not per packet.
 */
public abstract class NetworkCodec {

    // the name of the codec
    private final String name;

    public NetworkCodec(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /* Codec */

    public abstract void write(SocketNetworkHandler handler,
                               Protocol protocol,
                               Packet packet,
                               ObjectOutputStream stream,
                               boolean encrypt) throws Throwable;

    public abstract Packet read(SocketNetworkHandler handler,
                                Protocol protocol,
                                ObjectInputStream stream) throws Throwable;

}
