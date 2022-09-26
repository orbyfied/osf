package net.orbyfied.osf.resource.impl;

import net.orbyfied.osf.resource.AbstractResourceService;
import net.orbyfied.osf.resource.ServerResource;
import net.orbyfied.osf.resource.ServerResourceManager;
import net.orbyfied.osf.resource.event.ResourceHandleAcquireEvent;
import net.orbyfied.osf.resource.event.ResourceHandleReleaseEvent;
import net.orbyfied.osf.resource.event.ResourceUnloadEvent;
import net.orbyfied.j8.event.handler.BasicHandler;
import net.orbyfied.j8.util.functional.ThrowableRunnable;
import net.orbyfied.osf.util.data.IntBox;
import net.orbyfied.osf.util.worker.SafeWorker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResourceGCService extends AbstractResourceService {

    /**
     * Server resource type property key:
     * If set to false, the resource is not saved when
     * unloaded by the garbage collection service.
     * Default: True
     */
    public static final Object PERSISTENT = new Object();

    //////////////////////////////////////////////////////

    // if the garbage collector should automatically track
    // resource usages and act on them
    boolean automate = true;

    // the internal map for tracking which
    // resources are currently being used
    final ConcurrentHashMap<ServerResource, IntBox> usages = new ConcurrentHashMap<>();

    // the queue of resources to be unloaded async
    final Queue<ServerResource> queue = new ArrayDeque<>();
    // the queue worker
    final SafeWorker    worker  = new SafeWorker();
    final AtomicBoolean waiting = new AtomicBoolean(false);

    public ResourceGCService(ServerResourceManager manager) {
        super(manager);
        this.worker.withTarget(new WorkerTarget());
    }

    public SafeWorker worker() {
        return worker;
    }

    @Override
    public void added() {
        super.added();
        worker.commence();
    }

    @Override
    public void removed() {
        super.removed();
        worker.terminate();
    }

    /* ----- Manual Handling ---- */

    public void disposeImmediate(ServerResource resource) {
        // save resource if persistent
        if (resource.type().properties().getOrDefaultFlat(PERSISTENT, true)) {
            manager.saveResource(resource);
        }

        // unload resource
        manager.unloadResource(resource);
    }

    /**
     * Forces a resource to be marked acquired.
     * Use with caution, if not released manually
     * it will not be automatically disposed.
     * @param resource The resource.
     */
    public void acquireImmediate(ServerResource resource) {
        // increment usage
        IntBox u = usages.get(resource);
        if (u == null) {
            u = new IntBox();
            usages.put(resource, u);
        }
        u.value++;
    }

    /**
     * Forces a resource to be released by one.
     * @param resource The resource.
     */
    public void releaseImmediate(ServerResource resource) {
        // get counter from map
        IntBox u = usages.get(resource);
        boolean dispose = false;
        if (u != null) {
            // decrement usage
            u.value--;
            // check if we should dispose
            if (u.value <= 0)
                dispose = true;
        } else {
            // always dispose if its not in the map
            dispose = true;
        }

        // dispose if needed
        if (dispose)
            disposeImmediate(resource);
    }

    /* ----- Automatic Handling ----- */

    @BasicHandler
    void handleUnloaded(ResourceUnloadEvent event) {
        // remove from registries
        usages.remove(event.getResource());
    }

    @BasicHandler
    void handleReleased(ResourceHandleReleaseEvent event) {
        // check automated
        if (!automate)
            return;

        ServerResource resource = event.getHandle().getOrNull();
        if (resource == null)
            return;
        releaseImmediate(resource);
    }

    @BasicHandler
    void handleAcquired(ResourceHandleAcquireEvent event) {
        // check automated
        if (!automate)
            return;

        ServerResource resource = event.getHandle().getOrNull();
        if (resource == null)
            return;
        acquireImmediate(resource);
    }

    /* ------ Worker ------ */

    class WorkerTarget implements ThrowableRunnable {

        @Override
        public void run() throws Throwable {
            // while active
            while (worker.shouldRun()) {
                // wait for content
                if (queue.isEmpty()) {
                    synchronized (queue) {
                        waiting.set(true);
                        queue.wait();
                    }

                    waiting.set(false);
                }

                // handle all resources in queue
                List<ServerResource> resources = new ArrayList<>(queue.size());
                synchronized (queue) {
                    while (!queue.isEmpty())
                        resources.add(queue.poll());
                }

                for (ServerResource resource : resources) {
                    disposeImmediate(resource);
                }
            }
        }

    }

}
