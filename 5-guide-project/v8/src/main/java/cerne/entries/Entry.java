package cerne.entries;

import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import cerne.statements.EntryVisitor;

// SMELL 3: mutable java.util.Date, double for money and String type with magic values
@Entity
@Inheritance
public abstract class Entry {

    public static final String CREDIT = "C";
    public static final String DEBIT = "D";
    public static final String FEE = "T";

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "account_number")
    private String accountNumber;

    @Temporal(TemporalType.DATE)
    private Date date;

    private String type; // "C", "D" or "T"

    private double amount;

    private String description;

    protected Entry() {
    }

    protected Entry(String type, String accountNumber, double amount, Date date) {
        this.type = type;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.date = date;
    }

    /** Depends on the JVM default time zone. */
    public boolean isInCurrentMonth() {
        Calendar today = Calendar.getInstance();
        Calendar entryDate = Calendar.getInstance();
        entryDate.setTime(date);
        return entryDate.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                && entryDate.get(Calendar.YEAR) == today.get(Calendar.YEAR);
    }

    public boolean isCredit() {
        return CREDIT.equals(type);
    }

    /** Visitor double dispatch: each subclass calls the right visit method. */
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

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
