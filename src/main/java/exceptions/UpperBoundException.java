package exceptions;

/**
 * Created by $Hamid on 3/5/2017.
 */
public class UpperBoundException extends Exception {
    public UpperBoundException() {
        super();
    }

    public UpperBoundException(String message) {
        super(message);
    }

    public UpperBoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public UpperBoundException(Throwable cause) {
        super(cause);
    }
}
