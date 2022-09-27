package net.orbyfied.msgs.common.protocol;

import net.orbyfied.msgs.common.protocol.MessagePacket;
import net.orbyfied.osf.server.ProtocolSpecification;

public class MessageProtocolSpec extends ProtocolSpecification {

    @Override
    protected void create() {
        toCompile.add(MessagePacket.class);
    }

}
