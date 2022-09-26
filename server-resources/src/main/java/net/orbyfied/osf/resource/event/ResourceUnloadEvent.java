package net.orbyfied.osf.resource.event;

import net.orbyfied.osf.resource.ServerResource;
import net.orbyfied.osf.resource.ServerResourceManager;

public class ResourceUnloadEvent extends ResourceEvent {

    public ResourceUnloadEvent(ServerResourceManager manager, ServerResource resource) {
        super(manager, resource);
    }

}
