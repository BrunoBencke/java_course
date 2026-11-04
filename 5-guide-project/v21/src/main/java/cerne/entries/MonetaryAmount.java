package cerne.entries;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Rule shared by the compact constructors: amount present, positive and with two decimal places. */
final class MonetaryAmount {

    private MonetaryAmount() {
    }

    static BigDecimal positive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("entry amount must be positive: " + amount);
        }
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }
}
