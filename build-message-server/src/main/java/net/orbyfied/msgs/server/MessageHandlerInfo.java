package net.orbyfied.msgs.server;

import net.orbyfied.osf.server.ServerClient;

import java.util.UUID;

public record MessageHandlerInfo(UUID uuid, ServerClient client, int typeHash) {

}
