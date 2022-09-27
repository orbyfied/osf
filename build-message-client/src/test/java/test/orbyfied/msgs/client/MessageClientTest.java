package test.orbyfied.msgs.client;

import net.orbyfied.msgs.client.MessageAPI;
import net.orbyfied.msgs.client.MessageClient;
import net.orbyfied.msgs.common.Message;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

public class MessageClientTest {

    @Test
    void test() {
        MessageClient client = new MessageClient();
        client.connect(new InetSocketAddress("localhost", 9669));
        MessageAPI api = client.getAPI();

        api.on("name_message_b", (messageHandler, message) -> {
            message.values(v -> {
                System.out.println("Message received: " + v.getFlat("name"));
            });
        });
    }

}
