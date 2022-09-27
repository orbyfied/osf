package net.orbyfied.osf.client.event;

import net.orbyfied.osf.client.Client;

public class ClientReadyEvent extends ClientEvent {

    public ClientReadyEvent(Client client) {
        super(client);
    }

}
