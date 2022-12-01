package net.orbyfied.osf.service;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class ServiceManager {

    /**
     * The registered services by class.
     */
    final Map<Class<?>, Service> serviceMap = new Object2ObjectOpenHashMap<>();

    /**
     * The registered services.
     */
    final List<Service> services = new ArrayList<>();

    /**
     * The service adapter providers.
     */
    final List<ServiceAdapterProvider<?, ?>> adapterProviders = new ArrayList<>();

    /**
     * The service adapter providers by class.
     */
    final Map<Class<?>, ServiceAdapterProvider<?, ?>> adapterProviderMap = new Object2ObjectOpenHashMap<>();

    public <S extends Service> LoadBuilder<S> load(Class<S> sClass) {
        return new LoadBuilder<>(new ServiceProvider<>(this, sClass));
    }

    /////////////////////////////////////////////////////////////////

    @SuppressWarnings("rawtypes")
    public class LoadBuilder<S extends Service> {

        public LoadBuilder(ServiceProvider<S> provider) {
            // set provider
            this.provider = provider;

            // create adapter builders
            for (ServiceAdapterProvider adapterProvider : adapterProviders) {
                if (adapterProvider.builderClass == null) continue;
                adapterBuilderMap.put(adapterProvider.builderClass,
                        adapterProvider.newBuilder());
            }
        }

        // the service provider
        ServiceProvider<S> provider;

        // the arguments
        Object[] args = new Object[0];

        // the adapter builders
        Map<Class<?>, ServiceAdapterBuilder<?>> adapterBuilderMap = new Object2ObjectOpenHashMap<>();

        /**
         * Set the arguments to construct the service
         * object with. The amount and types should
         * correspond with the constructor you want
         * to call.
         */
        public LoadBuilder<S> constructWith(Object... args) {
            this.args = args;
            return this;
        }

        /**
         * Configure an adapter using the builder class.
         * If the provided builder class is not registered
         * the consumer won't be called.
         */
        @SuppressWarnings("unchecked")
        public <B extends ServiceAdapterBuilder> LoadBuilder<S> adapter(Class<B> bClass,
                                                                        BiConsumer<LoadBuilder<S>, B> bConsumer) {
            ServiceAdapterBuilder builder = adapterBuilderMap.get(bClass);
            if (builder != null)
                bConsumer.accept(this, (B) builder);
            return this;
        }

        /**
         * Configure an adapter using the builder class.
         * If the provided builder class is not registered
         * the consumer won't be called.
         */
        @SuppressWarnings("unchecked")
        public <B extends ServiceAdapterBuilder> LoadBuilder<S> adapter(Class<B> bClass,
                                                                        Consumer<B> bConsumer) {
            ServiceAdapterBuilder builder = adapterBuilderMap.get(bClass);
            if (builder != null)
                bConsumer.accept((B) builder);
            return this;
        }

        /**
         * Creates and loads the service.
         * @return The provider.
         */
        public ServiceProvider<S> create() {
            provider.create(this);
            return provider;
        }

    }

}
