package net.orbyfied.osf.resource.event;

import net.orbyfied.osf.resource.ServerResourceHandle;
import net.orbyfied.osf.resource.ServerResourceManager;

@SuppressWarnings("rawtypes")
public class ResourceHandleReleaseEvent extends ResourceHandleEvent {

    public ResourceHandleReleaseEvent(ServerResourceManager manager, ServerResourceHandle handle) {
        super(manager, handle);
    }

}
