package cerne.accounts;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

import cerne.members.Member;
import cerne.members.MemberType;

/**
 * An account is an entity (identity and mutable balance): it does not become a record. The accessors follow
 * the record style (number(), balance()) so they read like the rest of the domain.
 */
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

    protected Account() {
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

    public void block() {
        active = false;
    }

    public void changeBranch(Branch branch) {
        this.branch = branch;
    }

    public void changeDailyLimit(BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public void changeOverdraft(BigDecimal limit) {
        this.overdraftLimit = limit;
    }

    public String number() {
        return number;
    }

    public Branch branch() {
        return branch;
    }

    public Member holder() {
        return holder;
    }

    public MemberType holderType() {
        return holder.getType();
    }

    public BigDecimal balance() {
        return balance;
    }

    public boolean active() {
        return active;
    }

    public BigDecimal dailyLimit() {
        return dailyLimit;
    }

    /** Optional as a return type, not as a field: the field can still be null. */
    public Optional<BigDecimal> overdraftLimit() {
        return Optional.ofNullable(overdraftLimit);
    }
}
