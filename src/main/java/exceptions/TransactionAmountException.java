package exceptions;

/**
 * Created by $Hamid on 3/5/2017.
 */
public class TransactionAmountException extends Exception {
    public TransactionAmountException() {
        super();
    }

    public TransactionAmountException(String message) {
        super(message);
    }

    public TransactionAmountException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionAmountException(Throwable cause) {
        super(cause);
    }
}
