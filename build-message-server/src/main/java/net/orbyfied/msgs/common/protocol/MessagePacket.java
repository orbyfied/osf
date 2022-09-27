package net.orbyfied.msgs.common.protocol;

import net.orbyfied.msgs.common.Message;
import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.PacketType;
import net.orbyfied.osf.util.Values;
import net.orbyfied.osf.util.data.DataBinary;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MessagePacket extends Packet {

    public static final PacketType<MessagePacket> TYPE =
            new PacketType<>(MessagePacket.class, "msgs/message")
            .serializer((type, packet, stream) -> {
                stream.writeInt(packet.getMessage().getTypeHash());
                DataBinary.writeValues(new ObjectOutputStream(stream), packet.getMessage().values());
            })
            .deserializer((type, stream) -> {
                int typeHash = stream.readInt();
                Values values = DataBinary.readValues(new ObjectInputStream(stream));
                return new MessagePacket(new Message(typeHash).values(values));
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
