package server;

import terminal.Terminal;
import terminal.TransactionRequest;
import terminal.TransactionResponse;
import exceptions.*;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by $Hamid on 3/5/2017.
 */
class RequestHandler extends Thread {
    private static List<Deposit> deposits;
    private static final Logger LOGGER = Logger.getLogger(RequestHandler.class.getName());
    private Socket socket;
    private Terminal terminal;

    static void setDepositList(List<Deposit> deposits) {
        RequestHandler.deposits = deposits;
    }

    static void setLoggerFileHandler(FileHandler fileHandler) {
        RequestHandler.LOGGER.addHandler(fileHandler);
    }

    public RequestHandler(Socket socket) {
        super();
        this.socket = socket;
    }

    @Override
    public void run() {
        LOGGER.log(Level.INFO, "{0}: getting terminal information.", this.getName());
        terminal = receiveTerminalInformation();

        LOGGER.log(Level.INFO, "{0}:{1}:{2}: receiving requests.", new Object[]{this.getName(), terminal.getType(), terminal.getId().toString()});
        receiveRequestsCycle();

        closeSocket();

        LOGGER.log(Level.INFO, "{0}:{1}:{2}: thread closed.", new Object[]{this.getName(), terminal.getType(), terminal.getId().toString()});
    }

    private void receiveRequestsCycle() {
        while (true) {
            TransactionRequest transactionRequest = null;
            try {
                transactionRequest = receiveRequest();
                Integer depositIndex = searchDeposits(transactionRequest.getDepositID());
                processRequest(depositIndex, transactionRequest);
                LOGGER.log(Level.INFO, "{0}:{1}:{2}: transaction request id#{3} was valid and done.", new Object[]{this.getName(), terminal.getType(), terminal.getId().toString(), transactionRequest.getId()});
                sendResponse(transactionRequest.getId(), true, null, "Transaction successfully done!");

            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "{0}:{1}:{2}: input data was not valid.", new Object[]{this.getName(), terminal.getType(), terminal.getId().toString()});
                sendResponse(-1, false, e, "Sent class is not permitted!");
            } catch (DepositNotFoundException | TransactionTypeNotSupportedException | DepositBalanceNotSufficientException | TransactionAmountException | UpperBoundException e) {
                LOGGER.log(Level.INFO, String.format("%s:%s:%s: transaction request id#%d was not valid.", this.getName(), terminal.getType(), terminal.getId().toString(), transactionRequest.getId()), e);
                sendResponse(transactionRequest.getId(), false, e, "Transaction was not successful!");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format("%s:%s:%s: stop receiving requests due to an exception or closed terminal.", this.getName(), terminal.getType(), terminal.getId().toString()), e);
                break;
            }
        }
    }

    private void closeSocket() {
        try {
            socket.close();
            LOGGER.log(Level.INFO, "{0}:{1}:{2}: socket closed.", new Object[]{this.getName(), terminal.getType(), terminal.getId().toString()});
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, String.format("%s:%s:%s: error while closing socket.", this.getName(), terminal.getType(), terminal.getId().toString()), e);
        }
    }

    private void sendObject(Object object) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
    }

    private void sendResponse(Integer id, Boolean accepted, Exception exception, String message) {
        try {
            TransactionResponse transactionResponse = new TransactionResponse(id, accepted, exception, message, new Date());
            sendObject(transactionResponse);
            LOGGER.log(Level.INFO, "{0}:{1}:{2}: response of transaction request id#{3} sent to terminal.", new Object[]{this.getName(), terminal.getType(), terminal.getId().toString(), id});
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("%s:%s:%s: sending response of request id#%d failed.", this.getName(), terminal.getType(), terminal.getId().toString(), id), e);
            throw new RuntimeException(e);
        }
    }


    private Object receiveObject() throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
        return objectInputStream.readObject();
    }

    private TransactionRequest receiveRequest() throws ClassNotFoundException {
        try {
            TransactionRequest transactionRequest = (TransactionRequest) receiveObject();
            LOGGER.log(Level.INFO, "{0}:{1}:{2}: request id#{3} received.", new Object[]{this.getName(), terminal.getType(), terminal.getId().toString(), transactionRequest.getId()});
            return transactionRequest;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("%s:%s:%s: request receiving failed.", this.getName(), terminal.getType(), terminal.getId().toString()), e);
            throw new RuntimeException(e);
        }
    }

    private Terminal receiveTerminalInformation() {
        try {
            Terminal terminal = (Terminal) receiveObject();
            LOGGER.log(Level.INFO, "{0}:{1}:{2}: terminal information received: id:{3}, type:{4}.", new Object[]{this.getName(), terminal.getType(), terminal.getId().toString(), terminal.getId().toString(), terminal.getType()});
            return terminal;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, String.format("%s:%s:%s: terminal information receiving failed. thread is closing.", this.getName(), terminal.getType(), terminal.getId().toString()), e);
            throw new RuntimeException(e);
        }
    }

    private Integer searchDeposits(Integer id) throws DepositNotFoundException {
        for (int i = 0; i < deposits.size(); i++) {
            if (deposits.get(i).getId().equals(id))
                return i;
        }
        throw new DepositNotFoundException();
    }

    private void processRequest(Integer depositIndex, TransactionRequest transactionRequest)
            throws TransactionAmountException, UpperBoundException, DepositBalanceNotSufficientException, TransactionTypeNotSupportedException {
        synchronized (deposits.get(depositIndex).lock) {
            if (transactionRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0)
                throw new TransactionAmountException();
            if (deposits.get(depositIndex).getUpperBound().compareTo(transactionRequest.getAmount()) < 0)
                throw new UpperBoundException();
            if (transactionRequest.getType() == TransactionRequest.TransactionType.deposit)
                deposits.get(depositIndex).setBalance(deposits.get(depositIndex).getBalance().add(transactionRequest.getAmount()));
            else if (transactionRequest.getType() == TransactionRequest.TransactionType.withdraw)
                if (BigDecimal.ZERO.compareTo(deposits.get(depositIndex).getBalance().subtract(transactionRequest.getAmount())) <= 0)
                    deposits.get(depositIndex).setBalance(deposits.get(depositIndex).getBalance().subtract(transactionRequest.getAmount()));
                else throw new DepositBalanceNotSufficientException();
            else throw new TransactionTypeNotSupportedException();
        }
    }
}
