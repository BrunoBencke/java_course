package cerne.entries;

import java.util.Date;

import javax.persistence.Entity;

import cerne.statements.EntryVisitor;

@Entity
public class FeeEntry extends Entry {

    private String feeCode;

    public FeeEntry() {
        setType(FEE);
    }

    public FeeEntry(String accountNumber, double amount, Date date) {
        super(FEE, accountNumber, amount, date);
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
