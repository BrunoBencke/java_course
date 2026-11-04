package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.Entity;

import cerne.statements.EntryVisitor;

@Entity
public class CreditEntry extends Entry {

    private String source;

    public CreditEntry() {
        setType(EntryType.CREDIT);
    }

    public CreditEntry(String accountNumber, BigDecimal amount, LocalDate date) {
        super(EntryType.CREDIT, accountNumber, amount, date);
    }

    @Override
    public <R> R accept(EntryVisitor<R> visitor) {
        return visitor.visitCredit(this);
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
