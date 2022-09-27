package net.orbyfied.msgs.client;

import net.orbyfied.j8.event.handler.BasicHandler;
import net.orbyfied.osf.client.Client;
import net.orbyfied.osf.client.event.ClientReadyEvent;
import net.orbyfied.osf.util.Version;

public class MessageClient extends Client {

    public MessageClient() {
        super("msgs", Version.of("0.1.0"));
        // set configurations
        configuration.put(K_ENABLE_ENCRYPTED,  true);
        configuration.put(K_ENFORCE_ENCRYPTED, true);
    }

    /*
        Client API
     */

    // the message api
    private MessageAPI api;

    public MessageAPI getAPI() {
        return api;
    }

    /*
        Client Implementation
     */

    @BasicHandler
    void clientReady(ClientReadyEvent event) {

    }

}
