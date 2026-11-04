package cerne.transfers;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A transfer as it appears in Cerne or in the central's daily file. */
public class TransferRecord {

    private final String identifier;
    private final BigDecimal amount;
    private final LocalDate date;

    public TransferRecord(String identifier, BigDecimal amount, LocalDate date) {
        this.identifier = identifier;
        this.amount = amount;
        this.date = date;
    }

    public String getIdentifier() {
        return identifier;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public String toString() {
        return identifier + " " + amount + " " + date;
    }
}
