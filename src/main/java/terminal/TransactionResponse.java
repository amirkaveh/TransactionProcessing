package terminal;

import java.io.Serializable;

/**
 * Created by $Hamid on 3/5/2017.
 */
public class TransactionResponse implements Serializable {
    private Integer id;
    private Exception exception;
    private String description;

    public TransactionResponse(Integer id, Exception exception, String description) {
        this.id = id;
        this.exception = exception;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public Exception getException() {
        return exception;
    }

    public String getDescription() {
        return description;
    }
}
