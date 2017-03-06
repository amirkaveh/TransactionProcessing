package terminal;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

/**
 * Created by $Hamid on 3/4/2017.
 */
public class Terminal extends Thread {
    private Integer id;
    private String type;
    private String outLog;
    private String serverIp;
    private Integer serverPort;
    private List<TransactionRequest> transactionRequests;
    private Socket socket;
    private final Logger LOGGER = Logger.getLogger(Terminal.class.getName());

    public void initialization(String dataFileName) throws ParserConfigurationException, SAXException, IOException {

        NodeList terminalNodeList = xmlToNodeList(dataFileName, "terminal");
        Node terminal = terminalNodeList.item(0);
        Element terminalElement = (Element) terminal;
        id = Integer.parseInt(terminalElement.getAttribute("id"));
        type = terminalElement.getAttribute("type");
        outLog = ((Element) terminalElement.getElementsByTagName("outLog").item(0)).getAttribute("path");
        serverIp = ((Element) terminalElement.getElementsByTagName("server").item(0)).getAttribute("ip");
        serverPort = Integer.parseInt(((Element) terminalElement.getElementsByTagName("server").item(0)).getAttribute("port"));
        NodeList transactionRequestNodeList = ((Element) terminalElement.getElementsByTagName("transactions").item(0)).getElementsByTagName("transactionRequest");
        transactionRequests = nodeListToTransactionRequestList(transactionRequestNodeList);
        setLogger();
        LOGGER.log(Level.INFO,"Initialization done.");
    }

    private void setLogger() throws IOException {

        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        if(handlers.length>0 && handlers[0] instanceof ConsoleHandler)
            rootLogger.removeHandler(handlers[0]);
        LOGGER.setLevel(Level.INFO);
        FileHandler fileHandler = new FileHandler(outLog);
        SimpleFormatter simpleFormatter = new SimpleFormatter();
        fileHandler.setFormatter(simpleFormatter);
        LOGGER.addHandler(fileHandler);

        LOGGER.log(Level.INFO,"Logger configured.");
    }
    private NodeList xmlToNodeList(String inputFileName, String elementsTagName) throws IOException, ParserConfigurationException, SAXException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(inputFileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputStream);
        doc.getDocumentElement().normalize();
        return doc.getElementsByTagName(elementsTagName);
    }

    private List<TransactionRequest> nodeListToTransactionRequestList(NodeList nodeList) {

        List<TransactionRequest> transactionRequests = new ArrayList<TransactionRequest>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Integer transactionID = Integer.parseInt(((Element) nodeList.item(i)).getAttribute("id"));
            TransactionRequest.TransactionType transactionType = TransactionRequest.TransactionType.valueOf(((Element) nodeList.item(i)).getAttribute("type"));
            BigDecimal transactionAmount = new BigDecimal(((Element) nodeList.item(i)).getAttribute("amount"));
            Integer depositID = Integer.parseInt(((Element) nodeList.item(i)).getAttribute("deposit"));
            TransactionRequest transactionRequest = new TransactionRequest(this.id, this.type, transactionID, transactionType, transactionAmount, depositID);
            transactionRequests.add(transactionRequest);
        }
        return transactionRequests;
    }

    @Override
    public void run() {

        try {
            LOGGER.log(Level.INFO,"socket is to begin connecting.");
            socket = new Socket(serverIp, serverPort);
            LOGGER.log(Level.INFO,"socket connected to {0}:{1}",new Object[]{serverIp,serverPort.toString()});
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"socket connecting to server failed!",e);
            throw new RuntimeException(e);
        }
        for(TransactionRequest transactionRequest: transactionRequests) {
            LOGGER.log(Level.INFO,"processing transaction request with id {0}",transactionRequest.getId());
            sendRequest(transactionRequest);
            TransactionResponse transactionResponse = receiveResponse();

            String responseMessage = transactionResponse.getDescription();
            if (transactionResponse.getException() != null)
                responseMessage += "; "+ transactionResponse.getException();
            LOGGER.log(Level.INFO,"Server response to transaction request with id {0} is: {1}", new Object[]{transactionResponse.getId(),responseMessage});
        }
        LOGGER.log(Level.INFO,"Transaction Requests processing finished.");
        try {
            socket.close();
            LOGGER.log(Level.INFO,"Socket closed.");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,"closing socket problem.",e);
            throw new RuntimeException(e);
        }
    }

    private void sendRequest(Integer transactionID, TransactionRequest.TransactionType transactionType, BigDecimal transactionAmount, Integer depositID) {
        TransactionRequest transactionRequest = new TransactionRequest(this.id, this.type, transactionID, transactionType, transactionAmount, depositID);
        sendRequest(transactionRequest);
    }

    private void sendRequest(TransactionRequest transactionRequest) {

        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            objectOutputStream.writeObject(transactionRequest);
            objectOutputStream.flush();
            LOGGER.log(Level.INFO,"transaction request with id {0} sent successfully to the server.",transactionRequest.getId());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"Transaction request with id {0} couldn't be sent to the server.",e);
            throw new RuntimeException(e);
        }
    }

    private TransactionResponse receiveResponse() {

        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            TransactionResponse transactionResponse = (TransactionResponse) objectInputStream.readObject();
            LOGGER.log(Level.INFO,"Response Received successfully.");
            return transactionResponse;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.WARNING,"Receiving response failed",e);
            throw new RuntimeException(e);
        }
    }

}
