package net.orbyfied.osf.service;

/**
 * An extension to a service to
 * add additional functionality.
 */
@SuppressWarnings("rawtypes")
public abstract class ServiceAdapter<P extends ServiceAdapterProvider> {

    /**
     * The provider which created and
     * assigned this adapter.
     */
    protected final P provider;

    /**
     * The service provider.
     */
    protected final ServiceProvider<?> service;

    public ServiceAdapter(P provider,
                          ServiceProvider<?> service) {
        this.provider = provider;
        this.service  = service;
    }

    public P getProvider() {
        return provider;
    }

    public ServiceProvider<?> getService() {
        return service;
    }

}
