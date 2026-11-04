package cerne.entries;

import java.util.Date;

import javax.persistence.Entity;

import cerne.statements.EntryVisitor;

@Entity
public class DebitEntry extends Entry {

    public DebitEntry() {
        setType(DEBIT);
    }

    public DebitEntry(String accountNumber, double amount, Date date) {
        super(DEBIT, accountNumber, amount, date);
    }

    @Override
    public <R> R accept(EntryVisitor<R> visitor) {
        return visitor.visitDebit(this);
    }
}
