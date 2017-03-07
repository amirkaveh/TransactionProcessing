package terminal;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by $Hamid on 3/4/2017.
 */
public class TransactionRequest implements Serializable {
    private Integer id;
    private TransactionType type;
    private BigDecimal amount;
    private Integer depositID;

    public enum TransactionType {deposit, withdraw}

    public TransactionRequest(Integer id, TransactionType type, BigDecimal amount, Integer depositID) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.depositID = depositID;
    }

    public Integer getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getDepositID() {
        return depositID;
    }


}
