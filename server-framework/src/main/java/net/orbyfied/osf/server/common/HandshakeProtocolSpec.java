package net.orbyfied.osf.server.common;

import net.orbyfied.osf.server.ProtocolSpecification;
import net.orbyfied.osf.server.common.protocol.handshake.PacketClientboundPubKey;
import net.orbyfied.osf.server.common.protocol.handshake.PacketServerboundClientKey;

public class HandshakeProtocolSpec extends ProtocolSpecification {

    public static final HandshakeProtocolSpec INSTANCE = new HandshakeProtocolSpec();

    ////////////////////////////////////////

    @Override
    protected void create() {
        toCompile.add(PacketClientboundPubKey.class);
        toCompile.add(PacketServerboundClientKey.class);
    }

}
