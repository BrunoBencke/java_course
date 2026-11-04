package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.Entity;

import cerne.statements.EntryVisitor;

@Entity
public class DebitEntry extends Entry {

    public DebitEntry() {
        setType(EntryType.DEBIT);
    }

    public DebitEntry(String accountNumber, BigDecimal amount, LocalDate date) {
        super(EntryType.DEBIT, accountNumber, amount, date);
    }

    @Override
    public <R> R accept(EntryVisitor<R> visitor) {
        return visitor.visitDebit(this);
    }
}
