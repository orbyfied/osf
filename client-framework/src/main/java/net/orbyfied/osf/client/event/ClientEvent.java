package net.orbyfied.osf.client.event;

import net.orbyfied.j8.event.BusEvent;
import net.orbyfied.osf.client.Client;
import net.orbyfied.osf.util.event.Cancellable;

public abstract class ClientEvent extends BusEvent implements Cancellable {

    private final Client client;

    public ClientEvent(Client client) {
        this.client = client;
    }

    public Client client() {
        return client;
    }

}
