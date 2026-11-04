package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Closed hierarchy: four types. In 21 the domain does not know it is "visited": no accept(). */
public sealed interface Entry permits Credit, Debit, Fee, Reversal {

    String account();

    BigDecimal amount();

    LocalDate date();

    EntryType type();

    default boolean isCredit() {
        return type().isInflow();
    }

    /** Between start and end, inclusive. */
    default boolean isBetween(LocalDate start, LocalDate end) {
        return !date().isBefore(start) && !date().isAfter(end);
    }
}
