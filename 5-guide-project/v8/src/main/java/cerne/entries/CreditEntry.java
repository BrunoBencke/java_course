package cerne.entries;

import java.util.Date;

import javax.persistence.Entity;

import cerne.statements.EntryVisitor;

// SMELL 6: inheritance as reuse; the subclass exists to inherit the parent's fields and repeat the type "C"
@Entity
public class CreditEntry extends Entry {

    private String source;

    public CreditEntry() {
        setType(CREDIT);
    }

    public CreditEntry(String accountNumber, double amount, Date date) {
        super(CREDIT, accountNumber, amount, date);
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
