package cerne.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Record sent by the other credit union. */
public record ExternalRecord(String identifier, BigDecimal amount, LocalDate date) {
}
