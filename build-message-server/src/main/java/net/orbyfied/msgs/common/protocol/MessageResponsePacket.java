package net.orbyfied.msgs.common.protocol;

import net.orbyfied.msgs.common.Message;
import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.PacketType;

import java.util.UUID;

public class MessageResponsePacket extends Packet {

    public static final PacketType<MessageResponsePacket> TYPE =
            new PacketType<>(MessageResponsePacket.class, "msgs/message_response")
            .serializer((type, packet, stream) -> {
                UUID uuid = packet.targetUUID;
                stream.writeLong(uuid.getMostSignificantBits());
                stream.writeLong(uuid.getLeastSignificantBits());
                MessagePacket.TYPE.serialize(new MessagePacket(packet.message), stream);
            })
            .deserializer((type, stream) -> {
                UUID       uuid = new UUID(stream.readLong(), stream.readLong());
                Message message = MessagePacket.TYPE.deserialize(stream).getMessage();
                return new MessageResponsePacket(uuid, message);
            });

    ////////////////////////////////////////////

    // the uuid of the message
    // the response it responding to
    private final UUID targetUUID;
    // the response message
    private final Message message;

    public MessageResponsePacket(UUID targetUUID, Message message) {
        super(TYPE);
        this.targetUUID = targetUUID;
        this.message = message;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public Message getMessage() {
        return message;
    }

}
