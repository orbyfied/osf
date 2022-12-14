package net.orbyfied.osf.util.logging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventLogs {

    List<EventLog>        eventLogs   = new ArrayList<>();
    Map<String, EventLog> eventLogMap = new HashMap<>();

    Consumer<EventLog> initializer;

    public List<EventLog> getEventLogs() {
        return eventLogs;
    }

    public EventLogs withInitializer(Consumer<EventLog> consumer) {
        initializer = consumer;
        return this;
    }

    public EventLogs forAll(Consumer<EventLog> consumer) {
        for (EventLog eventLog : eventLogs)
            consumer.accept(eventLog);
        return this;
    }

    public EventLogs add(EventLog log) {
        eventLogs.add(log);
        eventLogMap.put(log.getName(), log);
        return this;
    }

    public EventLog create(String name) {
        EventLog log = new EventLog(name);
        add(log);
        if (initializer != null)
            initializer.accept(log);
        return log;
    }

    public EventLog get(String name) {
        return eventLogMap.get(name);
    }

    public EventLog getOrCreate(String name) {
        EventLog log;
        if ((log = get(name)) != null)
            return log;
        return create(name);
    }

}
