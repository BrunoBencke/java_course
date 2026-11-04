package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Credit(String account, BigDecimal amount, LocalDate date, String source) implements Entry {

    public Credit {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(date, "date");
        amount = MonetaryAmount.positive(amount);
        source = source == null ? "" : source.strip();
    }

    @Override
    public EntryType type() {
        return EntryType.CREDIT;
    }
}
