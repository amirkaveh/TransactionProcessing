package server;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

/**
 * Created by $Hamid on 3/1/2017.
 */
public class Server extends Thread {
    private Integer portNumber;
    private String outLog;
    private List<Deposit> deposits;
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    public void initialization(String dataFileName) throws IOException {

        String inputString = readFile(dataFileName, "UTF-8");
        JSONObject jsonObject = new JSONObject(inputString);
        portNumber = jsonObject.getInt("port");
        deposits = getDeposits(jsonObject);
        RequestHandler.setDepositList(deposits);
        outLog = jsonObject.getString("outLog");
        FileHandler fileHandler = setLogger();
        RequestHandler.setLoggerFileHandler(fileHandler);
    }

    private FileHandler setLogger() throws IOException {

        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        if (handlers.length > 0 && handlers[0] instanceof ConsoleHandler)
            rootLogger.removeHandler(handlers[0]);
        LOGGER.setLevel(Level.INFO);
        FileHandler fileHandler = new FileHandler(outLog);
        SimpleFormatter simpleFormatter = new SimpleFormatter();
        fileHandler.setFormatter(simpleFormatter);
        LOGGER.addHandler(fileHandler);

        LOGGER.log(Level.INFO, "Logger configured.");
        return fileHandler;
    }

    private String readFile(String fileName, String encoding) throws IOException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        return IOUtils.toString(inputStream, encoding);
    }


    private List<Deposit> getDeposits(JSONObject jsonObject) {

        JSONArray arrayOfDeposits = (JSONArray) jsonObject.get("deposits");
        //System.out.println(arrayOfDeposits.toString());
        List<Deposit> deposits = new ArrayList<Deposit>();
        for (Object deposit : arrayOfDeposits) {
            String customer = ((JSONObject) deposit).getString("customer");
            Integer id = ((JSONObject) deposit).getInt("id");
            BigDecimal initialBalance = new BigDecimal(((JSONObject) deposit).getString("initialBalance"));
            BigDecimal upperBound = new BigDecimal(((JSONObject) deposit).getString(("upperBound")));
            deposits.add(new Deposit(id, customer, initialBalance, upperBound));
        }
        return deposits;
    }


    @Override
    public void run() {
        ServerSocket serverSocket = serverStart();

        LOGGER.log(Level.INFO, "Start listening for new terminals");
        serverAccept(serverSocket);
    }

    private ServerSocket serverStart() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(portNumber, 1000, null);
            LOGGER.log(Level.INFO, "Server started.");
            return serverSocket;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server starting failed.", e);
            throw new RuntimeException("Starting server error!", e);
        }
    }

    private void serverAccept(ServerSocket serverSocket) {
        while (true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
                LOGGER.log(Level.INFO, "New terminal connected form {0}:{1}", new Object[]{socket.getInetAddress(), socket.getPort() + ""});
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Server accepting new connections error.", e);
                throw new RuntimeException("Server accepting connection error!", e);
            }

            RequestHandler requestHandler = new RequestHandler(socket);
            requestHandler.start();
            LOGGER.log(Level.INFO, "{0} created to handle terminal connected from {1}:{2}", new Object[]{requestHandler.getName(), socket.getInetAddress(), socket.getPort() + ""});
        }
    }

}
