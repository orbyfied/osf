package net.orbyfied.osf.resource;

import net.orbyfied.j8.registry.Identifier;
import net.orbyfied.j8.util.functional.TriFunction;
import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.osf.db.Database;
import net.orbyfied.osf.db.DatabaseItem;
import net.orbyfied.osf.db.QueryPool;
import net.orbyfied.osf.util.Logging;
import net.orbyfied.osf.util.Values;

import java.lang.reflect.Constructor;
import java.lang.reflect.InaccessibleObjectException;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;

public abstract class ServerResourceType<R extends ServerResource> {

    public static <R extends ServerResource> ServerResourceType<R> ofChronoIds(final Class<R> rClass,
                                                                               final Identifier id,

                                                                               final TriFunction<ServerResourceManager, DatabaseItem, R, ResourceSaveResult> saveR,
                                                                               final TriFunction<ServerResourceManager, DatabaseItem, R, ResourceLoadResult> loadR) {
        return new ServerResourceType<>(id, rClass) {
            @Override
            public UUID createLocalID() {
                return new UUID(
                        System.currentTimeMillis() * /* type hash */ idHash,
                        System.nanoTime()
                );
            }

            @Override
            public ResourceSaveResult saveResource(ServerResourceManager manager, DatabaseItem dbItem, R resource) {
                return saveR.apply(manager, dbItem, resource);
            }

            @Override
            public ResourceLoadResult loadResource(ServerResourceManager manager, DatabaseItem dbItem, R resource) {
                return loadR.apply(manager, dbItem, resource);
            }
        };
    }

    public static <R extends ServerResource> ServerResourceType<R> ofChronoIds(final Class<R> rClass,
                                                                               final String id,

                                                                               final TriFunction<ServerResourceManager, DatabaseItem, R, ResourceSaveResult> saveR,
                                                                               final TriFunction<ServerResourceManager, DatabaseItem, R, ResourceLoadResult> loadR) {
        return ofChronoIds(rClass, Identifier.of(id),
                saveR, loadR);
    }

    /* ------------------------------------ */

    protected static final Logger LOGGER = Logging.getLogger("ServerResource");

    // a utility random instance for generating IDs
    protected static final Random RANDOM =
            new Random(System.currentTimeMillis() ^ System.nanoTime());

    //////////////////////////////////////

    // the type identifier
    final Identifier id;
    // the type identifier hash
    // cached for performance
    final int idHash;

    // the runtime resource type
    Class<R> resourceClass;
    // the resource instance constructor
    Constructor<R> constructor;

    // the miscellaneous type properties
    final Values props = new Values();

    public ServerResourceType(Identifier id,
                              Class<R> resourceClass) {
        this.id     = id;
        this.idHash = id.hashCode();

        this.resourceClass = resourceClass;

        // get constructor
        try {
            try {
                this.constructor = resourceClass.getDeclaredConstructor(UUID.class, UUID.class);
            } catch (NoSuchMethodException e) {
                this.constructor = resourceClass.getDeclaredConstructor(UUID.class, ServerResourceType.class, UUID.class);
            }
        } catch (NoSuchMethodException e) {
            LOGGER.err("No constructor (UUID, UUID) for resource type " + id + " (" + resourceClass.getName() + ")");
        } catch (InaccessibleObjectException e) {
            LOGGER.err("Unable to access constructor (UUID, UUID) for resource type " + id + " (" + resourceClass.getName() + ")");
            e.printStackTrace(Logging.ERR);
        } catch (Exception e) {
            LOGGER.err("Failed to get constructor (UUID, UUID) for resource type " + id + " (" + resourceClass.getName() + ")");
            e.printStackTrace(Logging.ERR);
        }
    }

    /* Getters */

    public Identifier getIdentifier() {
        return id;
    }

    public int getIdentifierHash() {
        return idHash;
    }

    public Class<R> getResourceClass() {
        return resourceClass;
    }

    public Values properties() {
        return props;
    }

    public ServerResourceType<R> properties(BiConsumer<ServerResourceType<R>, Values> consumer) {
        consumer.accept(this, props);
        return this;
    }

    /* -------- Functional --------- */

    /**
     * Creates a new local ID. This ID should have
     * close to no possibility of colliding with
     * another ID.
     * @return The local unique ID.
     */
    public abstract UUID createLocalID();

    public R newInstanceInternal(UUID uuid, UUID localId) {
        try {
            if (constructor.getParameterCount() == 2)
                return constructor.newInstance(uuid, localId);
            // pass type due to accidental invalid constructor
            return constructor.newInstance(uuid, this, localId);
        } catch (Exception e) {
            // rethrow error
            throw new RuntimeException(e);
        }
    }

    public R loadResourceLocal(ServerResourceManager manager,
                               UUID localId) {
        // find document
        DatabaseItem item = findDatabaseResourceLocal(manager, manager.requireDatabase(), localId);

        if (item != null) {
            // get properties
            UUID uuid = item.get("uuid", UUID.class);

            // construct instance
            R resource = newInstanceInternal(uuid, localId);

            // load data
            loadResourceSafe(manager, item, resource);

            // add loaded
            manager.addLoaded(resource);

            // return
            return resource;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public ServerResourceType<R> saveResource(ServerResourceManager manager,
                                              ServerResource resource) {
        // find document
        DatabaseItem item = manager.findOrCreateDatabaseResource(resource.universalID());

        // set properties
        item.set("uuid",    resource.universalID());
        item.set("localId", resource.localID());
        item.set("type",    getIdentifierHash());

        // save data
        saveResourceSafe(manager, item, (R) resource);

        // return
        return this;
    }

    public static record ResourceLoadResult(boolean success, Throwable t) {
        public static ResourceLoadResult ofSuccess() {
            return new ResourceLoadResult(true, null);
        }
    }

    public static record ResourceSaveResult(boolean success, Throwable t) {
        public static ResourceSaveResult ofSuccess() {
            return new ResourceSaveResult(true, null);
        }
    }

    /**
     * Finds the database item of the resource by UUID.
     * @param manager The manager.
     * @param uuid The resource UUID.
     * @return The item or null if absent.
     */
    public DatabaseItem findDatabaseResource(ServerResourceManager manager, UUID uuid) {
        return manager.findDatabaseResource(uuid);
    }

    /**
     * Finds the database item of the resource by local ID.
     * @param manager The resource manager.
     * @param database The database.
     * @param localId The resource' local ID.
     * @return The item or null if absent.
     */
    public DatabaseItem findDatabaseResourceLocal(ServerResourceManager manager,
                                                  Database database,
                                                  UUID localId) {
        QueryPool pool = manager.getLocalQueryPool();
        return pool.current(database)
                .querySync("find_resource_local", new Values()
                        .setFlat("localId", localId)
                        .setFlat("typeHash", this.getIdentifierHash())
                );
    }

    /**
     * Saves a resource to the database. This does not
     * push the data into the database, that must be
     * done manually if needed.
     * @param manager The resource manager.
     * @param dbItem The database item to save to.
     * @param resource The resource to save.
     * @return Result.
     */
    public abstract ResourceSaveResult saveResource(ServerResourceManager manager,
                                                    DatabaseItem dbItem,
                                                    R resource);

    /**
     * Loads a resource from the database. This does not
     * pull the data from database, that must be
     * done manually if needed.
     * @param manager The resource manager.
     * @param dbItem The database item to load from.
     * @param resource The resource to load to.
     * @return Result.
     */
    public abstract ResourceLoadResult loadResource(ServerResourceManager manager,
                                                    DatabaseItem dbItem,
                                                    R resource);

    /**
     * Safe wrapper.
     * @see ServerResourceType#saveResource(ServerResourceManager, DatabaseItem, ServerResource)
     */
    public ResourceSaveResult saveResourceSafe(ServerResourceManager manager,
                                               DatabaseItem dbItem,
                                               R resource) {
        final Logger logger = ServerResourceManager.LOGGER;

        try {
            // call save
            ResourceSaveResult result = saveResource(manager, dbItem, resource);

            // push data if successful
            if (result.success()) {
                dbItem.push();
            }

            return result;
        } catch (Exception e) {
            logger.err("Error while saving resource " + resource.universalID() + " of type " +
                    resource.type().id);
            e.printStackTrace(Logging.ERR);
            return new ResourceSaveResult(false, e);
        }
    }

    /**
     * Safe wrapper.
     * @see ServerResourceType#loadResource(ServerResourceManager, DatabaseItem, ServerResource)
     */
    public ResourceLoadResult loadResourceSafe(ServerResourceManager manager,
                                               DatabaseItem dbItem,
                                               R resource) {
        final Logger logger = ServerResourceManager.LOGGER;

        try {
            // pull data
            dbItem.pull();

            // call load and return
            return loadResource(manager, dbItem, resource);
        } catch (Exception e) {
            logger.err("Error while loading resource " + resource.universalID() + " of type " +
                    resource.type().id);
            e.printStackTrace(Logging.ERR);
            return new ResourceLoadResult(false, e);
        }
    }

}
