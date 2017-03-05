import client.Client;
import org.xml.sax.SAXException;
import server.Server;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by $Hamid on 3/4/2017.
 */
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, ParserConfigurationException, SAXException {
        Server server = new Server();
        server.initialization("core.json");
        server.start();
        TimeUnit.SECONDS.sleep(3);
        for (int i = 0; i < 1; i++) {
            Client client = new Client();
            client.initialization("terminal.xml");
            client.start();
            TimeUnit.MILLISECONDS.sleep(1);
        }

    }
}
