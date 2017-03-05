package exceptions;

/**
 * Created by $Hamid on 3/5/2017.
 */
public class TransactionTypeNotSupportedException extends Exception {
    public TransactionTypeNotSupportedException() {
        super();
    }

    public TransactionTypeNotSupportedException(String message) {
        super(message);
    }

    public TransactionTypeNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionTypeNotSupportedException(Throwable cause) {
        super(cause);
    }
}
