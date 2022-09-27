package test.orbyfied.osf.boostrap;

import net.orbyfied.osf.bootstrap.ServerBootstrap;
import net.orbyfied.osf.server.Server;
import net.orbyfied.osf.util.Version;

import java.net.InetSocketAddress;
import java.nio.file.Path;

public class SimpleBoostrapTest {

    public static class MyServer extends Server {

        public MyServer() {
            super("MyServer", Version.of("1.0.0"));
        }

    }

    public static void main(String[] args) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap
                .useConsoleArguments(args)
//                .loadMainConfiguration("config.yml", SimpleBoostrapTest.class, "/config-defaults.yml")
                .withServer(MyServer::new)
                .withDefaultPort(6969)
                .withDefaultAddress(new InetSocketAddress("0.0.0.0", 6969))
                .bootstrap()
                .awaitClose();
    }

}
