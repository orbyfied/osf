package net.orbyfied.osf.bootstrap;

import net.orbyfied.osf.server.Server;
import net.orbyfied.osf.util.Values;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Class for bootstrapping a server, it
 * does things like parse arguments, configuring
 * the server and starting the server.
 */
public class ServerBootstrap {

    /*
        General
     */

    // the working directory
    private Path workDir = Path.of("./");

    /*
        Arguments
     */

    // the arg parser
    private ArgParser argParser = new ArgParser();
    // the parsed arguments
    private Values args = new Values();

    public ServerBootstrap withArgumentParser(ArgParser parser) {
        this.argParser = parser;
        return this;
    }

    public ServerBootstrap configureDefaultArguments() {
        argParser
                .withArgument("work-dir", Path.class, false)
                .withArgument("connect",  String.class, false);

        return this;
    }

    public ServerBootstrap configureArguments(BiConsumer<ServerBootstrap, ArgParser> consumer) {
        consumer.accept(this, argParser);
        return this;
    }

    public ServerBootstrap useConsoleArguments(String[] cmdArgs) {
        // parse command line arguments
        try {
            args.putAll(argParser.parseConsoleArgs(cmdArgs));
        } catch (Exception e) {
            throw new ServerBootstrapException("Error while parsing console args", e);
        }

        // set properties
        workDir = args.getOrDefaultFlat("work-dir", Path.of("./"));

        // return
        return this;
    }

    public Values argumentValues() {
        return args;
    }

    /*
        Configuration
     */

    // the loaded configuration values
    Values mainConfig = new Values();

    public ServerBootstrap loadMainConfiguration(String file, Class<?> ref, String defaultResourceName) {
        try {
            mainConfig.putAll(YamlConfig.copyDefaultsAndLoad(workDir.resolve(file), ref, defaultResourceName));
        } catch (Exception e) {
            throw new ServerBootstrapException("Error while loading main configuration\n" +
                    " [file: " + file + ", defaults: " + defaultResourceName + "]"
                    , e);
        }

        // return
        return this;
    }

    public Values getMainConfiguration() {
        return mainConfig;
    }

    public ServerBootstrap withConfigurationValue(Object key, Object value) {
        args.setFlat(key, value);
        return this;
    }

    /*
        Server
     */

    private SocketAddress defaultAddress;
    private Integer       defaultPort;

    // the server instance
    private Server server;

    public Server getServer() {
        return server;
    }

    public ServerBootstrap withServer(Server server) {
        this.server = server;
        return this;
    }

    public ServerBootstrap withServer(Supplier<Server> constructor) {
        try {
            withServer(constructor.get());
            return this;
        } catch (Exception e) {
            throw new ServerBootstrapException("Error while constructing server", e);
        }
    }

    public ServerBootstrap withDefaultAddress(SocketAddress address) {
        this.defaultAddress = address;
        return this;
    }

    public ServerBootstrap withDefaultPort(int port) {
        this.defaultPort = port;
        return this;
    }

    /**
     * Starts the server.
     * @return This.
     */
    public ServerBootstrap bootstrap() {
        // check for server
        if (server == null)
            throw new ServerBootstrapException("No server was created");

        // open server
        try {
            // get and parse address
            SocketAddress address = null;
            String  adrStr = args.getOrDefaultFlat("connect", mainConfig.getFlat("address"));
            if (adrStr != null) {
                String[] parts = adrStr.split(":");
                if (parts.length < 1)
                    throw new ServerBootstrapException("Invalid host address specified: " + adrStr);
                String   host = parts[0];
                Integer  port = null;
                if (parts.length > 1) {
                    port = Integer.parseInt(parts[1]);
                } else {
                    if (defaultPort == null) {
                        if (defaultAddress == null) {
                            throw new ServerBootstrapException("No host address port was specified");
                        }

                        address = defaultAddress;
                    } else {
                        port = defaultPort;
                    }
                }

                if (port != null)
                    address = new InetSocketAddress(host, port);
            } else {
                if (defaultAddress == null) {
                    throw new ServerBootstrapException("No host address was specified");
                }

                address = defaultAddress;
            }

            // connect server
            server.open(address);
        } catch (Exception e) {
            throw new ServerBootstrapException("Error while connecting server", e);
        }

        // prepare server
        try {
            server.prepare();
        } catch (Exception e) {
            throw new ServerBootstrapException("Error while preparing server", e);
        }

        // start server
        try {
            server.start();
        } catch (Exception e) {
            throw new ServerBootstrapException("Error while activating server", e);
        }

        // return
        return this;
    }

    /**
     * Waits for the server to shutdown.
     * This blocks the main thread until
     * that happens.
     * @return This.
     */
    public ServerBootstrap awaitClose() {
        server.serverSocketWorker().await(); // TODO: make better
        return this;
    }

}
