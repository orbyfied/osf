package net.orbyfied.osf.service.communication;

import net.orbyfied.osf.service.ServiceAdapter;
import net.orbyfied.osf.service.ServiceProvider;
import net.orbyfied.osf.service.communication.api.RemoteService;

public class CommunicationAdapter extends ServiceAdapter<CommunicationAdapterProvider> {

    // the remote service annotation
    RemoteService desc;

    public CommunicationAdapter(CommunicationAdapterProvider provider,
                                ServiceProvider<?> service,
                                RemoteService desc) {
        super(provider, service);
        this.desc = desc;
    }

}
