package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.Entity;

import cerne.statements.EntryVisitor;

@Entity
public class FeeEntry extends Entry {

    private String feeCode;

    public FeeEntry() {
        setType(EntryType.FEE);
    }

    public FeeEntry(String accountNumber, BigDecimal amount, LocalDate date) {
        super(EntryType.FEE, accountNumber, amount, date);
    }

    @Override
    public <R> R accept(EntryVisitor<R> visitor) {
        return visitor.visitFee(this);
    }

    public String getFeeCode() {
        return feeCode;
    }

    public void setFeeCode(String feeCode) {
        this.feeCode = feeCode;
    }
}
