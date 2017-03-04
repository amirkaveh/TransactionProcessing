package server;

import java.math.BigDecimal;

/**
 * Created by $Hamid on 3/1/2017.
 */
public class Deposit {
    private Integer id;
    private String customerName;
    private BigDecimal Balance;
    private BigDecimal upperBound;

    public Deposit(Integer id, String customerName, BigDecimal balance, BigDecimal upperBound) {
        this.id = id;
        this.customerName = customerName;
        Balance = balance;
        this.upperBound = upperBound;
    }

    public Integer getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getBalance() {
        return Balance;
    }

    public BigDecimal getUpperBound() {
        return upperBound;
    }
}
