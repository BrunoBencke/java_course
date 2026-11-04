package cerne.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

import cerne.entries.Credit;
import cerne.entries.Entry;
import cerne.entries.EntryType;

/**
 * Entry as it leaves the API. Record: equals covers every component (including source), no setters and
 * no Lombok. The compact constructor also runs during JSON deserialization.
 */
public record EntryDTO(String accountId, LocalDate date, BigDecimal amount, EntryType type, String source) {

    public EntryDTO {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(type, "type");
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive: " + amount);
        }
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        source = source == null ? "" : source.strip();
    }

    public static EntryDTO from(Entry entry) {
        // pattern matching for instanceof: no cast
        String source = entry instanceof Credit credit ? credit.source() : "";
        return new EntryDTO(entry.account(), entry.date(), entry.amount(), entry.type(), source);
    }
}
