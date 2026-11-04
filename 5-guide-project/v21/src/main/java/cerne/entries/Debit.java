package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Debit(String account, BigDecimal amount, LocalDate date) implements Entry {

    public Debit {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(date, "date");
        amount = MonetaryAmount.positive(amount);
    }

    @Override
    public EntryType type() {
        return EntryType.DEBIT;
    }
}
