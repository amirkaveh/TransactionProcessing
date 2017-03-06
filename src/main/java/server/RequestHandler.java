package server;

import terminal.TransactionRequest;
import terminal.TransactionResponse;
import exceptions.*;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by $Hamid on 3/5/2017.
 */
class RequestHandler extends Thread {
    private static List<Deposit> deposits;
    private static final Logger LOGGER = Logger.getLogger(RequestHandler.class.getName());
    private Socket socket;

    static void setDepositList(List<Deposit> deposits) {
        RequestHandler.deposits = deposits;
    }

    static void setLoggerFileHandler(FileHandler fileHandler) {
        RequestHandler.LOGGER.addHandler(fileHandler);
    }

    RequestHandler(Socket socket) {
        super();
        this.socket = socket;
    }

    @Override
    public void run() {
        LOGGER.log(Level.INFO, "{0}: receiving requests.", this.getName());
        while (true) {
            TransactionRequest transactionRequest = null;
            try {
                transactionRequest = receiveRequest();
                Integer depositIndex = searchDeposits(transactionRequest.getDepositID());
                processRequest(depositIndex, transactionRequest);
                LOGGER.log(Level.INFO,"{0}: transaction request id#{1} was valid and done.",new Object[]{this.getName(),transactionRequest.getId()});
                sendResponse(transactionRequest.getId(), null, "Transaction successfully done!");

            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING,"{0}: input data was not valid.",this.getName());
                sendResponse(-1, e, "Sent class is not permitted!");
            } catch (DepositNotFoundException | TransactionTypeNotSupportedException | DepositBalanceNotSufficientException | TransactionAmountException | UpperBoundException e) {
                LOGGER.log(Level.WARNING,String.format("%s: transaction request id#%d was not valid.",this.getName(),transactionRequest.getId()),e);
                sendResponse(transactionRequest.getId(), e, "Transaction was not successful!");
            } catch (Exception e) {
//                System.out.println(e.getCause());
//                if(e.getCause() instanceof EOFException)
//                    break;
                LOGGER.log(Level.WARNING,String.format("%s: closing socket due to an exception or closed terminal.",this.getName()),e);
                closeSocket();
                break;
            }
        }
        LOGGER.log(Level.INFO,"{0}: closing thread.",this.getName());
    }

    private void closeSocket() {
        try {
            socket.close();
            LOGGER.log(Level.INFO,"{0}: socket closed.",this.getName());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,String.format("%s: error while closing socket.",this.getName()),e);
        }
    }

    private void sendResponse(Integer id, Exception exception, String message) {
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            TransactionResponse transactionResponse = new TransactionResponse(id, exception, message);
            objectOutputStream.writeObject(transactionResponse);
            objectOutputStream.flush();
            LOGGER.log(Level.INFO,"{0}: response of transaction request {1} sent to terminal.",new Object[]{this.getName(),id});
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,String.format("%s: sending response of request %d failed.",this.getName(),id),e);
            throw new RuntimeException(e);
        }

    }

    private TransactionRequest receiveRequest() throws ClassNotFoundException {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            TransactionRequest transactionRequest = (TransactionRequest) objectInputStream.readObject();
            LOGGER.log(Level.INFO,"{0}: request id#{1} received.",new Object[]{ this.getName(), transactionRequest.getId()});
            return transactionRequest;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,String.format("%s: request receiving failed.",this.getName()),e);
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
