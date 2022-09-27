package net.orbyfied.osf.client.event;

import net.orbyfied.osf.client.Client;

public class ClientConnectEvent extends ClientEvent {

    public ClientConnectEvent(Client client) {
        super(client);
    }

}
