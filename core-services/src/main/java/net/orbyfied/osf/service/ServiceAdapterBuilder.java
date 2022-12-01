package net.orbyfied.osf.service;

/**
 * Builder for configuration of a service adapter.
 * @param <P> The adapter provider.
 */
@SuppressWarnings("rawtypes")
public abstract class ServiceAdapterBuilder<P extends ServiceAdapterProvider> {

    /**
     * The adapter provider which has created
     * and registered this builder.
     */
    protected final P provider;

    public ServiceAdapterBuilder(P provider) {
        this.provider = provider;
    }

}
