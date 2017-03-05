package exceptions;

/**
 * Created by $Hamid on 3/5/2017.
 */
public class DepositBalanceNotSufficientException extends Exception {
    public DepositBalanceNotSufficientException() {
        super();
    }

    public DepositBalanceNotSufficientException(String message) {
        super(message);
    }

    public DepositBalanceNotSufficientException(String message, Throwable cause) {
        super(message, cause);
    }

    public DepositBalanceNotSufficientException(Throwable cause) {
        super(cause);
    }
}
