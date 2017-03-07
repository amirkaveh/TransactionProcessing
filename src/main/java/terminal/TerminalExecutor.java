package terminal;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.*;

/**
 * Created by $Hamid on 3/4/2017.
 */
public class TerminalExecutor extends Thread {
    private Terminal terminal;
    private String outLog;
    private String serverIp;
    private Integer serverPort;
    private List<TransactionRequest> transactionRequests;
    private Socket socket;
    private final Logger LOGGER = Logger.getLogger(TerminalExecutor.class.getName());
    private Document doc;

    public void initialization(String dataFileName) throws ParserConfigurationException, SAXException, IOException {
        Element terminalElement = xmlGetTerminalElement(dataFileName);
        terminal = elementMakeTerminal(terminalElement);
        outLog = elementGetChildAttribute(terminalElement, "outLog", "path");
        serverIp = elementGetChildAttribute(terminalElement, "server", "ip");
        serverPort = Integer.parseInt(elementGetChildAttribute(terminalElement, "server", "port"));
        NodeList transactionRequestNodeList = elementGetChildElements(terminalElement, "transactions", "transaction");
        transactionRequests = nodeListToTransactionRequestList(transactionRequestNodeList);

        setLogger();
        LOGGER.log(Level.INFO, "Initialization done.");
    }

    private Terminal elementMakeTerminal(Element element) {
        Integer id = Integer.parseInt(element.getAttribute("id"));
        String type = element.getAttribute("type");
        return new Terminal(id, type);
    }

    private String elementGetChildAttribute(Element element, String childTagName, String attribute) {
        return ((Element) element.getElementsByTagName(childTagName).item(0)).getAttribute(attribute);
    }

    private NodeList elementGetChildElements(Element element, String childTagName, String targetTagName) {
        return ((Element) element.getElementsByTagName(childTagName).item(0)).getElementsByTagName(targetTagName);
    }

    private Element xmlGetTerminalElement(String inputFileName) throws ParserConfigurationException, SAXException, IOException {
        NodeList terminalNodeList = xmlGetNodeList(inputFileName, "terminal");
        Node terminalNode = terminalNodeList.item(0);
        return (Element) terminalNode;
    }

    private NodeList xmlGetNodeList(String inputFileName, String elementsTagName) throws IOException, ParserConfigurationException, SAXException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(inputFileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputStream);
        doc.getDocumentElement().normalize();
        return doc.getElementsByTagName(elementsTagName);
    }

    private List<TransactionRequest> nodeListToTransactionRequestList(NodeList nodeList) {
        List<TransactionRequest> transactionRequests = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Integer transactionID = Integer.parseInt(((Element) nodeList.item(i)).getAttribute("id"));
            TransactionRequest.TransactionType transactionType = TransactionRequest.TransactionType.valueOf(((Element) nodeList.item(i)).getAttribute("type"));
            BigDecimal transactionAmount = new BigDecimal(((Element) nodeList.item(i)).getAttribute("amount"));
            Integer depositID = Integer.parseInt(((Element) nodeList.item(i)).getAttribute("deposit"));
            TransactionRequest transactionRequest = new TransactionRequest(transactionID, transactionType, transactionAmount, depositID);
            transactionRequests.add(transactionRequest);
        }
        return transactionRequests;
    }

    private void setLogger() throws IOException {
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        if (handlers.length > 0 && handlers[0] instanceof ConsoleHandler)
            rootLogger.removeHandler(handlers[0]);
        LOGGER.setLevel(Level.INFO);
        FileHandler fileHandler = new FileHandler(outLog, false);
        SimpleFormatter simpleFormatter = new SimpleFormatter();
        fileHandler.setFormatter(simpleFormatter);
        LOGGER.addHandler(fileHandler);

        LOGGER.log(Level.INFO, "Logger configured.");
    }


    @Override
    public void run() {
        LOGGER.log(Level.INFO, "socket is to begin connecting.");
        connectToServer();
        LOGGER.log(Level.INFO, "terminal is introducing itself to server.");
        sendTerminalInformation();
        LOGGER.log(Level.INFO, "preparing xml interface to write responses.");
        Element xmlTransactionsNode = xmlWriteStart();
        LOGGER.log(Level.INFO, "terminal is starting sending transactions.");
        processTransactions(xmlTransactionsNode);
        LOGGER.log(Level.INFO, "finalizing writing to xml file.");
        xmlWriteFinish();
        LOGGER.log(Level.INFO, "transaction requests processing finished.");
        disconnectFromServer();
    }

    private void processTransactions(Element xmlTransactionsNode) {
        for (TransactionRequest transactionRequest : transactionRequests) {
            LOGGER.log(Level.INFO, "processing transaction request with id {0}", transactionRequest.getId());
            Boolean requestSent = sendRequest(transactionRequest);
            TransactionResponse transactionResponse = null;
            if (requestSent)
                transactionResponse = receiveResponse();

            xmlWriteTransactionResult(transactionRequest, transactionResponse, xmlTransactionsNode);

            if (transactionResponse != null) {
                String responseMessage = transactionResponse.getDescription();
                if (!transactionResponse.getAccepted())
                    responseMessage += "; " + transactionResponse.getException();
                LOGGER.log(Level.INFO, "Server response to transaction request with id {0} is: {1}", new Object[]{transactionRequest.getId(), responseMessage});
            } else {
                LOGGER.log(Level.WARNING, "Server response to transaction request with id {0} is not accessible due to transaction exchanging problem.", transactionRequest.getId());
            }
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(serverIp, serverPort);
            LOGGER.log(Level.INFO, "socket connected to {0}:{1}", new Object[]{serverIp, serverPort.toString()});
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "socket connecting to server failed!", e);
            throw new RuntimeException(e);
        }
    }

    private void disconnectFromServer() {
        try {
            socket.close();
            LOGGER.log(Level.INFO, "socket closed.");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "closing socket problem.", e);
            //throw new RuntimeException(e);
        }
    }

    private void sendTerminalInformation() {
        try {
            sendObject(this.terminal);
            LOGGER.log(Level.INFO, "Terminal information sent successfully to the server.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Terminal information couldn't be sent to the server.", e);
            throw new RuntimeException(e);
        }
    }

    private void sendObject(Object object) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
    }

    private Boolean sendRequest(TransactionRequest transactionRequest) {
        try {
            sendObject(transactionRequest);
            LOGGER.log(Level.INFO, "transaction request with id {0} sent successfully to the server.", transactionRequest.getId());
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Transaction request with id " + transactionRequest.getId() + " couldn't be sent to the server.", e);
            //throw new RuntimeException(e);
            return false;
        }
    }

    private Object receiveObject() throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
        return objectInputStream.readObject();
    }

    private TransactionResponse receiveResponse() {
        try {
            TransactionResponse transactionResponse = (TransactionResponse) receiveObject();
            LOGGER.log(Level.INFO, "Response Received successfully.");
            return transactionResponse;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "Receiving response failed", e);
            //throw new RuntimeException(e);
            return null;
        }
    }

    private Element xmlCreateElement(String tagName, Attr[] attrs, Node parent, String text) {
        if (attrs == null)
            return xmlCreateElement(tagName, (List<Attr>) null, parent, text);
        List<Attr> attrList = Arrays.asList(attrs);
        return xmlCreateElement(tagName, attrList, parent, text);
    }

    private Element xmlCreateElement(String tagName, List<Attr> attrs, Node parent, String text) {
        Element element = doc.createElement(tagName);
        parent.appendChild(element);
        if (attrs != null)
            for (Attr attr : attrs) {
                element.setAttributeNode(attr);
            }
        if (text != null && text.length() > 0)
            element.appendChild(doc.createTextNode(text));
        return element;
    }

    private Attr xmlCreateAttr(String name, String value) {
        Attr attr = doc.createAttribute(name);
        attr.setValue(value);
        return attr;
    }

    private Element xmlWriteStart() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();

            Element rootElement = xmlCreateElement("terminal", new Attr[]{xmlCreateAttr("id", terminal.getId().toString()), xmlCreateAttr("type", terminal.getType())}, doc, null);
            return xmlCreateElement("transactions", (Attr[]) null, rootElement, "");
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void xmlWriteTransactionResult(TransactionRequest request, TransactionResponse response, Element parent) {
        List<Attr> transactionAttrs = new ArrayList<>();
        transactionAttrs.add(xmlCreateAttr("id", request.getId().toString()));
        transactionAttrs.add(xmlCreateAttr("type", request.getType().toString()));
        transactionAttrs.add(xmlCreateAttr("amount", request.getAmount().toString()));
        transactionAttrs.add(xmlCreateAttr("deposit", request.getDepositID().toString()));
        Element transactionElement = xmlCreateElement("transaction", transactionAttrs, parent, null);

        if (response != null) {
            List<Attr> responseAttrs = new ArrayList<>();
            responseAttrs.add(xmlCreateAttr("accepted", response.getAccepted().toString()));
            Element responseElement = xmlCreateElement("response", responseAttrs, transactionElement, null);
            xmlCreateElement("description", (Attr[]) null, responseElement, response.getDescription());
            xmlCreateElement("time", (Attr[]) null, responseElement, formatDate(response.getTimeStamp()));
            if (response.getAccepted())
                xmlCreateElement("cause", (Attr[]) null, responseElement, " ");
            else
                xmlCreateElement("cause", (Attr[]) null, responseElement, response.getException().toString());
        } else {
            List<Attr> responseAttrs = new ArrayList<>();
            responseAttrs.add(xmlCreateAttr("accepted", "unknown"));
            Element responseElement = xmlCreateElement("response", responseAttrs, transactionElement, null);
            xmlCreateElement("description", (Attr[]) null, responseElement, "transaction exchanging between terminal and server faced some problems.");
            xmlCreateElement("cause", (Attr[]) null, responseElement, " ");
            xmlCreateElement("time", (Attr[]) null, responseElement, " ");
        }
    }

    private String formatDate(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return simpleDateFormat.format(date);
    }

    private void xmlWriteFinish() {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("response " + terminal.getId() + ".xml"));
            transformer.transform(source, result);
            LOGGER.log(Level.INFO, "writing transactions responses to \"{0}\" file was successful.", "response " + terminal.getId() + ".xml");
        } catch (TransformerException e) {
            LOGGER.log(Level.SEVERE, "writing transactions responses to \"{0}\" file failed.", "response " + terminal.getId() + ".xml");
            throw new RuntimeException(e);
        }
    }

}
