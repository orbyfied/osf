package net.orbyfied.osf.network;

import net.orbyfied.osf.network.handler.HandlerNode;
import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.osf.util.logging.EventLog;
import net.orbyfied.osf.util.logging.Logging;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Generic network handler.
 * @param <S> Self.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class NetworkHandler<S extends NetworkHandler> {

    /** The event log. */
    protected static final EventLog LOGGER = Logging.getEventLog("NetworkHandler");

    /**
     * The owner of this handler.
     */
    protected Object owner;

    // the network manager
    protected final NetworkManager manager;
    // the optional parent
    protected final NetworkHandler parent;

    // the handler node
    protected HandlerNode node = new HandlerNode(null);

    // worker
    protected AtomicBoolean active = new AtomicBoolean(true);
    protected WorkerThread workerThread;

    private final S self;

    /**
     * Create a new network handler bound
     * to a manager and optional parent.
     * @param manager The network manager.
     * @param parent The parent.
     *               Null if none.
     */
    public NetworkHandler(final NetworkManager manager,
                          final NetworkHandler parent) {
        this.manager = manager;
        this.parent  = parent;
        this.self    = (S) this;
    }

    /**
     * Set the owner of this handler.
     * @param owner The owner object.
     * @return This.
     */
    public S owned(Object owner) {
        this.owner = owner;
        return self;
    }

    /**
     * Get the network handler node.
     * @return The top node.
     */
    public HandlerNode node() {
        return node;
    }

    /**
     * Get the network manager.
     * @return The manager.
     */
    public NetworkManager manager() {
        return manager;
    }

    /**
     * Start this network handler.
     * This will create and start the worker.
     * @return This.
     */
    public S start() {
        // create worker thread
        if (workerThread == null)
            workerThread = createWorkerThread();
        // quit if still null
        if (workerThread == null)
            return self;

        // set active
        active.set(true);
        workerThread.start();
        return self;
    }

    /**
     * Deactivate the network handler.
     * @return This.
     */
    public S stop() {
        active.set(false);
        return self;
    }

    /**
     * Called when a fatal error occurs
     * causing the handler to exit.
     * @return This.
     */
    protected S fatalClose() {
        // doesnt do anything by default
        return self;
    }

    /**
     * Check if the handler is active.
     * @return Boolean.
     */
    public boolean active() {
        return active.get();
    }

    /**
     * Creates the worker thread.
     * @return The worker.
     */
    protected abstract WorkerThread createWorkerThread();

    /**
     * Handles an incoming packet.
     * @param packet The packet.
     */
    protected void handle(Packet packet) {
        // call handler node
        if (this.node != null)
            this.node.handle(this, packet);

        // call parent
        if (parent != null)
            parent.handle(packet);
    }

    /* ---- Worker ---- */

    public abstract class WorkerThread extends Thread {
        static int id = 0;

        public WorkerThread() {
            super("NHWorker-" + (id++));
        }

        @Override
        public void run() {
            try {
                runSafe();
            } catch (Throwable t) {
                fatalClose();
                LOGGER.newErr("socket_worker_loop", this.getName() + ": Error in socket worker network loop")
                        .withError(t)
                        .push();
                t.printStackTrace();
            }

            // make sure inactive status
            active.set(false);
        }

        /**
         * Should run actual code.
         * Abstracts away the error handling.
         */
        public abstract void runSafe() throws Throwable;
    }

}
