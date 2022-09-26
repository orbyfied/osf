package net.orbyfied.osf.db.impl;

import com.mongodb.client.MongoClient;
import net.orbyfied.osf.db.Database;
import net.orbyfied.osf.db.DatabaseManager;
import net.orbyfied.osf.db.DatabaseType;

public class MongoDatabase extends Database {

    public MongoDatabase(DatabaseManager manager, String name) {
        super(manager, name, DatabaseType.MONGO_DB);
    }

    // mongo client
    protected MongoClient client;
    // mongo database client
    protected com.mongodb.client.MongoDatabase db;

    public MongoClient getClient() {
        return client;
    }

    public com.mongodb.client.MongoDatabase getDatabaseClient() {
        return db;
    }

    @Override
    public boolean isOpen() {
        return db != null;
    }

}
