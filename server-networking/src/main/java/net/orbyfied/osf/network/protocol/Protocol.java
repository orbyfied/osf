package net.orbyfied.osf.network.protocol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public class Protocol<I extends ProtocolInstance> {

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

    // the instance constructor
    // wont create an instance if null
    BiFunction<Protocol, Object, I> instanceConstructor;

    // the instances
    final HashMap<Object, I> instances = new HashMap<>();

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

    public void destroyInstance(ProtocolInstance instance) {
        // get owner
        Object owner = instance.getOwner();

        // remove from instances
        instances.remove(owner);

        // remove from parents
        Protocol cp = this;
        while ((cp = cp.getParent()) != null) {
            cp.instances.remove(owner);
        }
    }

    @SuppressWarnings("unchecked")
    public I getInstance(Object owner) {
        // check map
        I instance;
        if ((instance = instances.get(owner)) != null)
            return instance;

        // create instance if there is a constructor
        if (instanceConstructor != null) {
            // create instance
            instance = instanceConstructor.apply(this, owner);

            // instantiate parents
            Protocol         cp = this;
            ProtocolInstance ci = instance;
            while ((cp = cp.getParent()) != null) {
                if (cp.instanceConstructor != null) {
                    ProtocolInstance inst = (ProtocolInstance) cp.instanceConstructor
                            .apply(cp, owner);
                    cp.instances.put(owner, inst);
                    ci.parent = inst;
                    ci = inst;
                }
            }

            // register instance
            instances.put(instance.getOwner(), instance);

            // return instance
            return instance;
        }

        // return null
        return null;
    }

    public Protocol<I> withInstanceConstructor(BiFunction<Protocol, Object, I> function) {
        this.instanceConstructor = function;
        return this;
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
