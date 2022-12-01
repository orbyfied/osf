package net.orbyfied.osf.service;

/**
 * A service instance.
 *
 */
public class Service {

    /**
     * The service manager this service
     * has been instantiated by.
     */
    protected final ServiceManager serviceManager;

    /**
     * The service provider.
     */
    protected final ServiceProvider<?> serviceProvider;

    public Service(ServiceManager serviceManager,
                   ServiceProvider<?> serviceProvider) {
        this.serviceManager  = serviceManager;
        this.serviceProvider = serviceProvider;
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

}
