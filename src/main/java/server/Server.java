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

/**
 * Created by $Hamid on 3/1/2017.
 */
public class Server extends Thread {
    private Integer portNumber;
    private String outLog;
    private List<Deposit> deposits;

    public void initialization(String dataFileName) throws IOException {
        String inputString = readFile(dataFileName, "UTF-8");

        JSONObject jsonObject = new JSONObject(inputString);
        portNumber = jsonObject.getInt("port");
        deposits = getDeposits(jsonObject);
        RequestHandler.SetDepositList(deposits);
        outLog = jsonObject.getString("outLog");
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
        ServerSocket serverSocket;
        Socket socket;

        try {
            serverSocket = new ServerSocket(portNumber, 1000, null);
        } catch (IOException e) {
            throw new RuntimeException("Starting server error!", e);
        }
        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                throw new RuntimeException("Server accepting connection error!", e);
            }
            RequestHandler requestHandler = new RequestHandler(socket);
            requestHandler.start();
        }
    }

}
