package cerne.transfers;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A transfer as it appears in Cerne or in the central's daily file. */
public record TransferRecord(String identifier, BigDecimal amount, LocalDate date) {
}
