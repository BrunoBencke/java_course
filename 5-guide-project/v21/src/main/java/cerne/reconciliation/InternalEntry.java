package cerne.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A Cerne entry as it enters the reconciliation with the other credit union. */
public record InternalEntry(String identifier, BigDecimal amount, LocalDate date) {
}
