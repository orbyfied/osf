package net.orbyfied.msgs.common.protocol;

import net.orbyfied.msgs.common.Message;
import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.PacketType;
import net.orbyfied.osf.util.Values;
import net.orbyfied.osf.util.data.DataBinary;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

public class MessagePacket extends Packet {

    public static final PacketType<MessagePacket> TYPE =
            new PacketType<>(MessagePacket.class, "msgs/message")
            .serializer((type, packet, stream) -> {
                Message message = packet.message;
                UUID uuid = message.getUUID();
                stream.writeLong(uuid.getMostSignificantBits());
                stream.writeLong(uuid.getLeastSignificantBits());
                stream.writeInt(message.getTypeHash());
                DataBinary.writeValues(new ObjectOutputStream(stream), message.values());
            })
            .deserializer((type, stream) -> {
                UUID uuid = new UUID(stream.readLong(), stream.readLong());
                int typeHash = stream.readInt();
                Values values = DataBinary.readValues(new ObjectInputStream(stream));
                return new MessagePacket(new Message(uuid, typeHash).values(values));
            });

    /////////////////////////////////////////////

    // the values
    private final Message message;

    public MessagePacket(Message message) {
        super(TYPE);
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

}
