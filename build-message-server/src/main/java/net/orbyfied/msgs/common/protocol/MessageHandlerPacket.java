package net.orbyfied.msgs.common.protocol;

import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.network.PacketType;

import java.util.UUID;

public class MessageHandlerPacket extends Packet {

    public enum Action {

        /**
         * Remove one handler from the sending client
         * for the specified type.
         */
        REMOVE_1,

        /**
         * Add one handler from the sending client
         * for the specified type.
         */
        ADD_1,

        /**
         * Remove all handlers of the sending client.
         */
        REMOVE_ALL

    }

    public static final PacketType<MessageHandlerPacket> TYPE =
            new PacketType<>(MessageHandlerPacket.class, "msgs/handler")
            .serializer((type, packet, stream) -> {
                Action action = packet.action;
                stream.writeInt(action.ordinal());
                if (action == Action.ADD_1 || action == Action.REMOVE_1) {
                    stream.writeLong(packet.handlerUUID.getMostSignificantBits());
                    stream.writeLong(packet.handlerUUID.getLeastSignificantBits());
                    if (action == Action.ADD_1)
                        stream.writeInt(packet.getTypeHash());
                }
            })
            .deserializer((type, stream) -> {
                Action action = Action.values()[stream.readInt()];
                MessageHandlerPacket packet = new MessageHandlerPacket(action);
                if (action == Action.ADD_1 || action == Action.REMOVE_1) {
                    UUID uuid = new UUID(stream.readLong(), stream.readLong());
                    packet.setHandlerUUID(uuid);
                    if (action == Action.ADD_1)
                        packet.setTypeHash(stream.readInt());
                }
                return packet;
            });

    /////////////////////////////////////////////////////////

    Action action;
    UUID handlerUUID;
    int typeHash;

    public MessageHandlerPacket(Action action) {
        super(TYPE);
        this.action = action;
    }

    public MessageHandlerPacket setHandlerUUID(UUID handlerUUID) {
        this.handlerUUID = handlerUUID;
        return this;
    }

    public MessageHandlerPacket setTypeHash(int typeHash) {
        this.typeHash = typeHash;
        return this;
    }

    public UUID getHandlerUUID() {
        return handlerUUID;
    }

    public Action getAction() {
        return action;
    }

    public int getTypeHash() {
        return typeHash;
    }

}
