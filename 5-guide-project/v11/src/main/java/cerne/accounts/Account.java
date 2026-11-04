package cerne.accounts;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import cerne.members.Member;

/** Checking account: entity with identity and mutable state (balance, active). */
@Entity
public class Account {

    @Id
    private String number;
    @Transient
    private Branch branch;
    @ManyToOne
    private Member holder;
    private BigDecimal balance = BigDecimal.ZERO;
    private boolean active = true;
    private BigDecimal dailyLimit = new BigDecimal("10000.00");
    private BigDecimal overdraftLimit;

    public Account() {
    }

    public Account(String number, Branch branch, Member holder, BigDecimal balance) {
        this.number = number;
        this.branch = branch;
        this.holder = holder;
        this.balance = balance;
    }

    public void debit(BigDecimal amount) {
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public String getNumber() {
        return number;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Member getHolder() {
        return holder;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    /** May be null: usually only legal entities have an overdraft. */
    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }
}
