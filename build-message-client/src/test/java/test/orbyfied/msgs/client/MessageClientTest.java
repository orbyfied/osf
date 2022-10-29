package test.orbyfied.msgs.client;

import net.orbyfied.msgs.client.MessageAPI;
import net.orbyfied.msgs.client.MessageClient;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

public class MessageClientTest {

    @Test
    void testClientA() {
        MessageClient client = new MessageClient();
        client.connect(new InetSocketAddress("localhost", 9669));
        MessageAPI api = client.getAPI();

        api.listen("test_message_b", (messageHandler, message) -> {
            message.values(v -> {
                System.out.println("Message B received: " + v.getFlat("test"));
                api.remove(messageHandler);
            });
        });

        api.send(api.create("test_message_a")
                .values(v -> v.put("test", "Hello Cunt!")));
    }

    @Test
    void testClientB() {
        MessageClient client = new MessageClient();
        client.connect(new InetSocketAddress("localhost", 9669));
        MessageAPI api = client.getAPI();

        api.listen("test_message_a", (messageHandler, message) -> {
            message.values(v -> {
                System.out.println("Message A received: " + v.getFlat("test"));
                api.send(api.create("test_message_b")
                        .values(v1 -> v1.put("test", "Hello Motherfucker!")));
                api.send(api.create("test_message_b")
                        .values(v1 -> v1.put("test", "Hello Motherfucker 2!")));
            });
        });
    }

}
