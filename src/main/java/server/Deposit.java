package server;

import java.math.BigDecimal;

/**
 * Created by $Hamid on 3/1/2017.
 */
class Deposit {
    private Integer id;
    private String customerName;
    private BigDecimal Balance;
    private BigDecimal upperBound;
    final Object lock = new Object();

    Deposit(Integer id, String customerName, BigDecimal balance, BigDecimal upperBound) {
        this.id = id;
        this.customerName = customerName;
        Balance = balance;
        this.upperBound = upperBound;
    }

    Integer getId() {
        return id;
    }

    String getCustomerName() {
        return customerName;
    }

    BigDecimal getBalance() {
        return Balance;
    }

    BigDecimal getUpperBound() {
        return upperBound;
    }

    void setBalance(BigDecimal balance) {
        Balance = balance;
    }
}
