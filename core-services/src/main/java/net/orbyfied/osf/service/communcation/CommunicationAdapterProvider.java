package net.orbyfied.osf.service.communcation;

import net.orbyfied.osf.service.ServiceAdapterProvider;
import net.orbyfied.osf.service.ServiceManager;
import net.orbyfied.osf.service.ServiceProvider;
import net.orbyfied.osf.service.communcation.api.RemoteService;

public class CommunicationAdapterProvider extends ServiceAdapterProvider<CommunicationAdapter, CommunicationAdapterBuilder> {

    public CommunicationAdapterProvider(ServiceManager manager) {
        super(manager, CommunicationAdapter.class, CommunicationAdapterBuilder.class);
    }

    @Override
    protected CommunicationAdapterBuilder newBuilder() {
        return new CommunicationAdapterBuilder(this);
    }

    @Override
    protected CommunicationAdapter newAdapter(ServiceProvider<?> serviceProvider,
                                              CommunicationAdapterBuilder builder) {
        // check for remote service annotation
        RemoteService desc;
        if ((desc = serviceProvider.getServiceClass().getAnnotation(RemoteService.class)) == null)
            return null;

        // create new adapter
        return new CommunicationAdapter(this, serviceProvider, desc);
    }

}
