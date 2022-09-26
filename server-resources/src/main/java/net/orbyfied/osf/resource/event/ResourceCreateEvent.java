package net.orbyfied.osf.resource.event;

import net.orbyfied.osf.resource.ServerResource;
import net.orbyfied.osf.resource.ServerResourceManager;

public class ResourceCreateEvent extends ResourceEvent {

    public ResourceCreateEvent(ServerResourceManager manager, ServerResource resource) {
        super(manager, resource);
    }

}
