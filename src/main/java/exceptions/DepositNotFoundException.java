package exceptions;

/**
 * Created by $Hamid on 3/5/2017.
 */
public class DepositNotFoundException extends Exception {
    public DepositNotFoundException() {
        super();
    }

    public DepositNotFoundException(String message) {
        super(message);
    }

    public DepositNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public DepositNotFoundException(Throwable cause) {
        super(cause);
    }
}
