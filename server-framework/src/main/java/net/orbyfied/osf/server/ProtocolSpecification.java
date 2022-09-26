package net.orbyfied.osf.server;

import net.orbyfied.osf.network.NetworkManager;
import net.orbyfied.osf.network.Packet;

import java.util.ArrayList;
import java.util.List;

public abstract class ProtocolSpecification {

    protected final List<Class<? extends Packet>> toCompile = new ArrayList<>();

    public ProtocolSpecification() {
        // create protocol
        create();
    }

    public void apply(NetworkManager manager) {
        for (Class<? extends Packet> klass : toCompile) {
            manager.compilePacketClass(klass);
        }
    }

    protected abstract void create();

}
