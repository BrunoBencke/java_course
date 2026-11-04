package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;

/** It used to be 72 lines with the type field forgotten in equals; now it is impossible to forget. */
public record EntrySummary(long id, LocalDate date, BigDecimal amount, EntryType type) {
}
