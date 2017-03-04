package client;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.Socket;

/**
 * Created by $Hamid on 3/4/2017.
 */
public class Client extends Thread {

    @Override
    public void run() {
        Socket socket;
        ObjectOutputStream objectOutputStream;
        try {
            socket = new Socket("127.0.0.1", 8086);
            objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            Transaction transaction = new Transaction(12, Transaction.TransactionType.deposit, BigDecimal.ONE, 12);
            objectOutputStream.writeObject(transaction);
            objectOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
