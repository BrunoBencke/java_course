package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Returns to the member the amount of an earlier entry. */
public record Reversal(String account, BigDecimal amount, LocalDate date, UUID originalEntry) implements Entry {

    public Reversal {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(originalEntry, "originalEntry");
        amount = MonetaryAmount.positive(amount);
    }

    @Override
    public EntryType type() {
        return EntryType.REVERSAL;
    }
}
