package cerne.transfers;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Validation shared by the transfers' compact constructors. */
final class TransferValues {

    private TransferValues() {
    }

    static BigDecimal positive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("transfer amount must be positive: " + amount);
        }
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    static String notBlank(String text, String field) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return text.strip();
    }
}
