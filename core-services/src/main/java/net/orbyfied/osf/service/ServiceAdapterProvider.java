package net.orbyfied.osf.service;

/**
 * A provider which creates the service
 * adapter to assign to each service.
 */
@SuppressWarnings("rawtypes")
public abstract class ServiceAdapterProvider<A extends ServiceAdapter, B extends ServiceAdapterBuilder> {

    /**
     * The service manager.
     */
    protected final ServiceManager manager;

    /**
     * The runtime adapter class.
     */
    protected final Class<A> adapterClass;

    /**
     * The runtime builder class.
     */
    protected final Class<B> builderClass;

    public ServiceAdapterProvider(ServiceManager manager,
                                  Class<A> adapterClass,
                                  Class<B> builderClass) {
        this.manager = manager;

        this.adapterClass = adapterClass;
        this.builderClass = builderClass;
    }

    public ServiceManager getManager() {
        return manager;
    }

    public Class<A> getAdapterClass() {
        return adapterClass;
    }

    public Class<B> getBuilderClass() {
        return builderClass;
    }

    ////////////////////////////////////////////////////

    /**
     * Creates a new adapter builder instance.
     * @return The adapter builder.
     */
    protected abstract B newBuilder();

    /**
     * Creates a new service adapter to
     * be assigned to the given service.
     * If the return value is null no
     * adapter will be assigned.
     * @param serviceProvider The service.
     * @param builder The adapter builder for configuration.
     * @return An adapter or null to ignore.
     */
    protected abstract A newAdapter(ServiceProvider<?> serviceProvider,
                                    B builder);

}
