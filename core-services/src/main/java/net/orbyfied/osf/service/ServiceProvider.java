package net.orbyfied.osf.service;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class which hold (internal) data about a service
 * and manages/provides it.
 * @param <S> The service type.
 */
@SuppressWarnings("rawtypes")
public final class ServiceProvider<S extends Service> {

    /**
     * The service manager.
     */
    final ServiceManager manager;

    /**
     * The service class.
     */
    Class<S> serviceClass;

    /**
     * The service instance.
     */
    S service;

    /**
     * The service adapters by class.
     */
    final Map<Class<?>, ServiceAdapter> adapterMap = new Object2ObjectOpenHashMap<>();

    /**
     * The service adapters.
     */
    final List<ServiceAdapter> adapters = new ArrayList<>();

    public ServiceProvider(ServiceManager manager,
                           Class<S> serviceClass) {
        this.manager      = manager;
        this.serviceClass = serviceClass;
    }

    /**
     * (Re)creates the instance of the service and
     * all of the adapters from the service class
     * and service managers adapter providers.
     * @param loadBuilder The load configuration.
     * @return This.
     */
    @SuppressWarnings("unchecked")
    public ServiceProvider<S> create(ServiceManager.LoadBuilder<S> loadBuilder) {
        try {
            // get constructor
            Constructor<?> constructor = null;
            constructors: for (Constructor<?> c : serviceClass.getDeclaredConstructors()) {
                // check parameter count
                if (c.getParameterCount() != 2 + loadBuilder.args.length) continue;
                Class<?>[] paramTypes = c.getParameterTypes();

                // check core parameters
                if (paramTypes[0] != ServiceManager.class || paramTypes[1] != ServiceProvider.class)
                    continue;

                // check arguments
                for (int i = 2; i < paramTypes.length; i++) {
                    Object arg = loadBuilder.args[i - 2];
                    if (arg == null) continue;
                    if (!paramTypes[i].isAssignableFrom(arg.getClass()))
                        continue constructors;
                }

                // found valid constructor
                constructor = c;
                break;
            }

            // throw exception if no constructor
            if (constructor == null)
                throw new NoSuchMethodException("Could not find constructor for (ServiceManager, ServiceProvider, args...)");

            // instantiate
            Object[] args = new Object[loadBuilder.args.length + 2];
            args[0] = manager; args[1] = this;
            System.arraycopy(loadBuilder.args, 0, args, 2, loadBuilder.args.length);
            service = (S) constructor.newInstance(args);
        } catch (Exception e) {
            // rethrow exception
            throw new ServiceCreateException("Failed to create service instance for: " + serviceClass, e);
        }

        // create adapters
        for (ServiceAdapterProvider adapterProvider : manager.adapterProviders) {
            // create instance
            ServiceAdapter<?> adapter = adapterProvider.newAdapter(this,
                    loadBuilder.adapterBuilderMap.get(adapterProvider.builderClass));

            // register if not null
            if (adapter != null) {
                adapters.add(adapter);
                adapterMap.put(adapterProvider.adapterClass, adapter);
            }
        }

        // return
        return this;
    }

    /**
     * Destroys the service and adapters.
     * @return This.
     */
    public ServiceProvider<S> destroy() {
        // TODO

        // return
        return this;
    }

    // Getters

    public S service() {
        return service;
    }

    public ServiceManager getManager() {
        return manager;
    }

    public Class<S> getServiceClass() {
        return serviceClass;
    }

    @SuppressWarnings("unchecked")
    public <T extends Service> T getServiceAs() {
        return (T) service;
    }

}
