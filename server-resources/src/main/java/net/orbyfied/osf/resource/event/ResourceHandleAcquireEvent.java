package net.orbyfied.osf.resource.event;

import net.orbyfied.osf.resource.ServerResourceHandle;
import net.orbyfied.osf.resource.ServerResourceManager;

@SuppressWarnings("rawtypes")
public class ResourceHandleAcquireEvent extends ResourceHandleEvent {

    public ResourceHandleAcquireEvent(ServerResourceManager manager, ServerResourceHandle handle) {
        super(manager, handle);
    }

}
