package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;

import cerne.statements.EntryVisitor;

/** Fixed while still on Java 8 (session 1 challenge): BigDecimal, LocalDate and enum. */
@Entity
@Inheritance
public abstract class Entry {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "account_number")
    private String accountNumber;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private EntryType type;

    private BigDecimal amount;

    private String description;

    protected Entry() {
    }

    protected Entry(EntryType type, String accountNumber, BigDecimal amount, LocalDate date) {
        this.type = type;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.date = date;
    }

    /** No hidden time zone: YearMonth of the entry itself. */
    public boolean isInCurrentMonth() {
        return YearMonth.from(date).equals(YearMonth.now());
    }

    public boolean isCredit() {
        return type == EntryType.CREDIT;
    }

    /** Between start and end, inclusive. */
    public boolean isBetween(LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    public abstract <R> R accept(EntryVisitor<R> visitor);

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public LocalDate getDate() {
        return date;
    }

    public EntryType getType() {
        return type;
    }

    protected void setType(EntryType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
