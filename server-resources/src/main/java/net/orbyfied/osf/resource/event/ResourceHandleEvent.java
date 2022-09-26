package net.orbyfied.osf.resource.event;

import net.orbyfied.osf.resource.ServerResource;
import net.orbyfied.osf.resource.ServerResourceHandle;
import net.orbyfied.osf.resource.ServerResourceManager;
import net.orbyfied.j8.event.BusEvent;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class ResourceHandleEvent extends BusEvent {

    // the resource manager
    final ServerResourceManager manager;
    // the resource handle
    final ServerResourceHandle handle;

    public ResourceHandleEvent(ServerResourceManager manager,
                               ServerResourceHandle handle) {
        this.manager = manager;
        this.handle  = handle;
    }

    public ServerResourceManager getManager() {
        return manager;
    }

    public ServerResourceHandle getHandle() {
        return handle;
    }

    public <R extends ServerResource> ServerResourceHandle<R> getHandleAs() {
        return (ServerResourceHandle<R>) handle;
    }

    public <R extends ServerResource> ServerResourceHandle<R> getHandleAs(Class<R> rClass) {
        return (ServerResourceHandle<R>) handle;
    }

}
