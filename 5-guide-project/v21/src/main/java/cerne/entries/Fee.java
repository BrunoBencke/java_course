package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Fee(String account, BigDecimal amount, LocalDate date, String feeCode) implements Entry {

    public Fee {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(date, "date");
        amount = MonetaryAmount.positive(amount);
        feeCode = feeCode == null ? "" : feeCode;
    }

    @Override
    public EntryType type() {
        return EntryType.FEE;
    }
}
