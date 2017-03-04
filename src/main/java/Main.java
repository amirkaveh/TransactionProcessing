import client.Client;
import server.Server;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by $Hamid on 3/4/2017.
 */
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = new Server();
        server.initialization("core.json");
        server.start();
        TimeUnit.SECONDS.sleep(3);
        Client client = new Client();
        client.start();

    }
}
