package test.orbyfied.osf.client;

import net.orbyfied.osf.client.Client;
import net.orbyfied.osf.network.Packet;
import net.orbyfied.osf.util.Version;

import java.net.InetSocketAddress;

public class SimpleClientTest {

    public static void main(String[] args) {

        Client client = new Client("MyClient", Version.of("33"));
        client.connect(new InetSocketAddress(6969));

    }

}
