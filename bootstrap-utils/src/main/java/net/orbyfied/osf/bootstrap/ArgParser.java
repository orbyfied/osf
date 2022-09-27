package net.orbyfied.osf.bootstrap;

import net.orbyfied.j8.util.Reader;
import net.orbyfied.j8.util.Sequence;
import net.orbyfied.j8.util.StringReader;
import net.orbyfied.osf.util.Values;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public class ArgParser {

    // args specification
    Map<String, Class<?>> spec = new HashMap<>();
    // positioned args specification
    List<Class<?>> posSpec = new ArrayList<>();
    // required argument names
    List<String> reqArgs = new ArrayList<>();

    // string parsers
    Map<Class<?>, Function<StringReader, Object>> parsers = new HashMap<>();

    {
        // add default parsers
        withParser(Boolean.class, reader -> Boolean.parseBoolean(reader.collect()));
        withParser(Float.class, reader -> Float.parseFloat(reader.collect()));
        withParser(Double.class, reader -> Double.parseDouble(reader.collect()));
        withParser(Integer.class, reader -> Integer.parseInt(reader.collect()));
        withParser(Long.class, reader -> Long.parseLong(reader.collect()));

        withParser(String.class, StringReader::collect);
        withParser(Path.class, reader -> Path.of(reader.collect()));
    }

    public ArgParser withArgument(String name, Class<?> type, boolean required) {
        withArgument(name, type);
        if (name != null && required)
            reqArgs.add(name);
        return this;
    }

    public ArgParser withArgument(String name, Class<?> type) {
        if (name == null)
            posSpec.add(type);
        else
            spec.put(name, type);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> ArgParser withParser(Class<T> type, Function<StringReader, T> function) {
        parsers.put(type, (Function<StringReader, Object>) function);
        return this;
    }

    public Values parseArgsUnchecked(List<String> args) {
        // prepare storage
        Values values = new Values();
        List<Object> positionedValues = new ArrayList<>(posSpec.size());
        Values externValues = new Values();
        values.setFlat("#pos", positionedValues);
        values.setFlat("#ext", externValues);

        Reader<String> argReader = new Reader<>(Sequence.ofList(args));
        int pi = 0; // positioned arg index
        String str;

        String key;
        while ((str = argReader.current()) != null) {
            // check if is named key
            if (str.startsWith("--")) /* named arg */ {
                // key
                key = str.substring(2);

                // get type from spec
                Class<?> type = spec.get(key);
                if (type == null)
                    throw new ArgParseException("unknown named arg '" + key + "' "
                            + pi + " ('" + str + "' @ " + argReader.index() + ")");
                Function<StringReader, Object> parser = parsers.get(type);
                if (parser == null)
                    throw new ArgParseException("dont know how to parse type '" + type.getName() + "'");

                // search for value
                String valStr = argReader.next();
                if (valStr == null)
                    throw new ArgParseException("expected value of type '" + type.getName() + "' for arg '" + key + "'");

                // parse value
                Object val;
                try {
                    val = parser.apply(new StringReader(valStr));
                } catch (Exception e) {
                    throw new ArgParseException("error while parsing arg '" + key
                            + "' of type '" + type.getName() + "'", e);
                }

                // put parsed value
                values.setFlat(key, val);
            } else if (str.startsWith("-")) /* misc value */ {
                // get operation and name
                char   op   = str.charAt(1);
                String name = str.substring(2);
                switch (op) {
                    // external value
                    case 'D', 'E' -> {
                        // search for value
                        String valStr = argReader.next();
                        if (valStr == null)
                            throw new ArgParseException("expected value for extern value spec '" + name + "'");

                        // put string value
                        externValues.setFlat(name, valStr);
                        values.setFlat("#E" + name, valStr);
                    }
                }
            } else /* positioned arg */ {
                // get type from spec
                if (pi >= posSpec.size())
                    throw new ArgParseException("unexpected positioned arg "
                            + pi + " ('" + str + "' @ " + argReader.index() + ")");
                Class<?> type = posSpec.get(pi);
                Function<StringReader, Object> parser = parsers.get(type);
                if (parser == null)
                    throw new ArgParseException("dont know how to parse type '" + type.getName() + "'");

                // parse value
                Object val;
                try {
                    val = parser.apply(new StringReader(str));
                } catch (Exception e) {
                    throw new ArgParseException("error while parsing positioned arg " + pi
                            + " of type '" + type.getName() + "'", e);
                }

                // put parsed value
                values.setFlat("#" + pi, val);
                positionedValues.add(val);

                // advance
                pi++;
            }

            // advance
            argReader.next();
        }

        // return values
        return values;
    }

    public Values checkRequired(Values values) {
        // check required args
        for (String rKey : reqArgs) {
            // check if the required argument is present
            if (!values.contains(rKey))
                throw new ArgParseException("required arg '" + rKey + "' is not present");
        }

        // return values
        return values;
    }

    public Values parseArgs(List<String> args) {
        return checkRequired(parseArgsUnchecked(args));
    }

    public Values parseConsoleArgs(String[] oldArgs) {
        // correctly parse args
        List<String> args = new ArrayList<>();
        {
            Reader<String> argReader = new Reader<>(Sequence.ofList(Arrays.asList(oldArgs)));
            String arg;
            StringBuilder buf = new StringBuilder();
            while ((arg = argReader.current()) != null) {
                // check starting character
                if (arg.startsWith("\"") || arg.startsWith("'")) {
                    // add text to buffer
                    buf.append(arg, 1, arg.length());
                } else if (arg.endsWith("\"") || arg.endsWith("'")) {
                    // add text to buffer
                    buf.append(" ").append(arg, 0, arg.length() - 1);

                    // add and clear buffer
                    args.add(buf.toString());
                    buf = new StringBuilder();
                } else {
                    args.add(arg);
                }

                // advance
                argReader.next();
            }
        }

        // parse args
        return parseArgs(args);
    }

    /* ----- Values ------ */

    @SuppressWarnings("unchecked")
    public <T> T parseValue(String str, Class<?> tClass) {
        if (str == null)
            return null;
        Function<StringReader, T> func = (Function<StringReader, T>) parsers.get(tClass);
        if (func == null)
            throw new UnsupportedOperationException("dont know how to parse type '" + tClass.getName() + "'");
        return func.apply(new StringReader(str));
    }

    public String getExtern(Values values, String name) {
        if (values == null)
            return null;
        return values.getFlat("#ext", Values.class).getFlat(name);
    }

    public <T> T getExtern(Values values, String name, Class<T> tClass) {
        if (values == null)
            return null;
        String str = values.getFlat("#ext", Values.class).getFlat(name);
        return parseValue(str, tClass);
    }

}
