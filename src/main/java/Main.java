import terminal.Terminal;
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
        //System.out.println(Thread.activeCount());
        Server server = new Server();
        server.initialization("core.json");
        server.start();
        TimeUnit.SECONDS.sleep(1);
        for (int i = 0; i < 2; i++) {
            Terminal terminal = new Terminal();
            terminal.initialization("terminal.xml");
            terminal.start();
            TimeUnit.SECONDS.sleep(1);
        }


    }
}
