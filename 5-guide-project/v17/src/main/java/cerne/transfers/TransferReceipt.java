package cerne.transfers;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferReceipt(String identifier, TransferRequest request,
                              BigDecimal fee, Instant instant) {
}
