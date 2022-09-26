package net.orbyfied.osf.db;

import net.orbyfied.j8.registry.Identifier;
import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.osf.util.Logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Database management.
 */
@SuppressWarnings("rawtypes")
public class DatabaseManager {

    public static final Logger LOGGER = Logging.getLogger("Databases");

    // databases
    final ArrayList<Database>       db = new ArrayList<>();
    final HashMap<String, Database> dbByName = new HashMap<>();

    // database types
    final ArrayList<DatabaseType<Database>>           dbt = new ArrayList<>();
    final HashMap<Identifier, DatabaseType<Database>> dbtById = new HashMap<>();

    public DatabaseManager() {
        // initialize default types
        addType(DatabaseType.MONGO_DB);
    }

    public List<Database> databases() {
        return Collections.unmodifiableList(db);
    }

    public List<DatabaseType<Database>> types() {
        return Collections.unmodifiableList(dbt);
    }

    @SuppressWarnings("unchecked")
    public <D extends Database> D getDatabase(String name) {
        return (D) dbByName.get(name);
    }

    public DatabaseManager addDatabase(Database database) {
        db.add(database);
        dbByName.put(database.name, database);
        return this;
    }

    public <D extends Database> DatabaseManager addDatabase(
            String name,
            BiFunction<DatabaseManager, String, D> constructor,
            Consumer<D> consumer
    ) {
        D db = constructor.apply(this, name);
        addDatabase(db);
        if (consumer != null)
            consumer.accept(db);
        return this;
    }

    public DatabaseManager addType(DatabaseType type) {
        dbt.add(type);
        dbtById.put(type.id, type);
        return this;
    }

    public QueryPool queryPool() {
        return new QueryPool(null);
    }

}
