package server;

import client.TransactionRequest;
import client.TransactionResponse;
import exceptions.*;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.List;

/**
 * Created by $Hamid on 3/5/2017.
 */
class RequestHandler extends Thread {
    private static List<Deposit> deposits;
    private Socket socket;

    static void SetDepositList(List<Deposit> deposits) {
        RequestHandler.deposits = deposits;
    }

    RequestHandler(Socket socket) {
        super();
        this.socket = socket;
    }

    @Override
    public void run() {
        TransactionRequest transactionRequest = null;
        try {
            transactionRequest = receiveRequest();
            Integer depositIndex = searchDeposits(transactionRequest.getDepositID());
            processRequest(depositIndex, transactionRequest);
            //System.out.println(deposits.get(depositIndex).getId() + ", " + deposits.get(depositIndex).getBalance());
            sendResponse(transactionRequest.getId(), null, "Transaction successfully done!");

        } catch (ClassNotFoundException e) {
            //throw new RuntimeException(e);
            sendResponse(transactionRequest.getId(), e, "Sent class is not permitted!");
        } catch (DepositNotFoundException | TransactionTypeNotSupportedException | DepositBalanceNotSufficientException | TransactionAmountException | UpperBoundException e) {
            //e.printStackTrace();
            sendResponse(transactionRequest.getId(), e, "Transaction was not successful!");
        }

    }

    private void sendResponse(Integer id, Exception exception, String message) {
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            TransactionResponse transactionResponse = new TransactionResponse(id, exception, message);
            objectOutputStream.writeObject(transactionResponse);
            objectOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private TransactionRequest receiveRequest() throws ClassNotFoundException {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            return (TransactionRequest) objectInputStream.readObject();
        } catch (IOException e) {
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
