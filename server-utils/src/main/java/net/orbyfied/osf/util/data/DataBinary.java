package net.orbyfied.osf.util.data;

import net.orbyfied.osf.util.Values;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class DataBinary {

    public static void writeValue(ObjectOutputStream stream, Object val) throws Exception {
        stream.writeObject(val);
    }

    public static Object readValue(ObjectInputStream stream) throws Exception {
        return stream.readObject();
    }

    public static void writeList(ObjectOutputStream stream, List list) throws Exception {
        stream.writeInt(list.size());
        for (Object o : list) {
            writeValue(stream, o);
        }
    }

    public static List readList(ObjectInputStream stream) throws Exception {
        int s = stream.readInt();
        List list = new ArrayList(s);
        for (int i = 0; i < s; i++) {
            list.add(readValue(stream));
        }
        return list;
    }

    public static void writeValues(ObjectOutputStream stream, Values values) throws Exception {
        for (Map.Entry<Object, Object> entry : values.entries()) {
            writeValue(stream, entry.getKey());
            writeValue(stream, entry.getValue());
            stream.writeBoolean(true);
        }
        stream.writeBoolean(false);
    }

    public static Values readValues(ObjectInputStream stream) throws Exception {
        Values values = new Values();
        boolean b;
        while (b = stream.readBoolean()) {
            Object key = readValue(stream);
            Object val = readValue(stream);
            values.put(key, val);
        }
        return values;
    }

}
