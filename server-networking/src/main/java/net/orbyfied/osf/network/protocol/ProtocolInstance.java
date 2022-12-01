package net.orbyfied.osf.network.protocol;

@SuppressWarnings("rawtypes")
public class ProtocolInstance {

    // the protocol this is an instance of
    private final Protocol protocol;

    // the object this instance was assigned to
    private final Object owner;

    // the parent instance
    ProtocolInstance parent;

    public ProtocolInstance(Protocol protocol, Object owner) {
        this.protocol = protocol;
        this.owner    = owner;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public Object getOwner() {
        return owner;
    }

    @SuppressWarnings("unchecked")
    public <I extends ProtocolInstance> I getParent() {
        return (I) parent;
    }

}
