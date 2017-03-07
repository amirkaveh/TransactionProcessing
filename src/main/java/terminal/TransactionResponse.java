package terminal;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by $Hamid on 3/5/2017.
 */
public class TransactionResponse implements Serializable {
    private Integer id;
    private Boolean accepted;
    private Exception exception;
    private String description;
    private Date timeStamp;

    public TransactionResponse(Integer id, Boolean accepted, Exception exception, String description, Date timeStamp) {
        this.id = id;
        this.accepted = accepted;
        this.exception = exception;
        this.description = description;
        this.timeStamp = timeStamp;
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

    public Boolean getAccepted() {
        return accepted;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }
}
