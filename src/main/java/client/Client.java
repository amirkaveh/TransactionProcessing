package client;

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

/**
 * Created by $Hamid on 3/4/2017.
 */
public class Client extends Thread {
    private Integer id;
    private String type;
    private String outLog;
    private String serverIp;
    private Integer serverPort;
    private List<TransactionRequest> transactionRequests;
    private Socket socket;

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

//        System.out.println(id);
//        System.out.println(type);
//        System.out.println(outLog);
//        System.out.println(serverIp);
//        System.out.println(serverPort);


    }

    private NodeList xmlToNodeList(String inputFileName, String elementsTagName) throws IOException, ParserConfigurationException, SAXException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(inputFileName);
        //File inputFile = new File(inputFileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputStream);
        doc.getDocumentElement().normalize();
        return doc.getElementsByTagName(elementsTagName);
    }

    private List<TransactionRequest> nodeListToTransactionRequestList(NodeList nodeList) {
        List<TransactionRequest> transactionRequests = new ArrayList<TransactionRequest>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Integer id = Integer.parseInt(((Element) nodeList.item(i)).getAttribute("id"));
            TransactionRequest.TransactionType type = TransactionRequest.TransactionType.valueOf(((Element) nodeList.item(i)).getAttribute("type"));
            BigDecimal amount = new BigDecimal(((Element) nodeList.item(i)).getAttribute("amount"));
            Integer depositID = Integer.parseInt(((Element) nodeList.item(i)).getAttribute("deposit"));
            TransactionRequest transactionRequest = new TransactionRequest(id, type, amount, depositID);
            transactionRequests.add(transactionRequest);
        }
        return transactionRequests;
    }

    @Override
    public void run() {
//        try {
//            socket = new Socket("127.0.0.1", 8086);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        try {
            socket = new Socket(serverIp, serverPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sendRequest(transactionRequests.get(0));
        //sendRequest(1, TransactionRequest.TransactionType.deposit, BigDecimal.ONE, 1000);

        TransactionResponse transactionResponse = receiveResponse();

        String exceptionMessage = "";
        if (transactionResponse.getException() != null)
            exceptionMessage = transactionResponse.getException() + ": ";
        System.out.println(exceptionMessage + transactionResponse.getDescription());
    }

    private void sendRequest(Integer transactionID, TransactionRequest.TransactionType type, BigDecimal transactionAmount, Integer depositID) {
        TransactionRequest transactionRequest = new TransactionRequest(transactionID, type, transactionAmount, depositID);
        sendRequest(transactionRequest);
    }

    private void sendRequest(TransactionRequest transactionRequest) {
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            objectOutputStream.writeObject(transactionRequest);
            objectOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TransactionResponse receiveResponse() {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            return (TransactionResponse) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


}
