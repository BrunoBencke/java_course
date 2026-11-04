package cerne.transfers;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fee by transfer type, in Java 17's two honest forms (session 4 challenge):
 * calculate = switch expression over the enum discriminator, no default;
 * calculateFee = instanceof ladder with pattern matching, no cast, with the throw that only 21 removes.
 */
public class FeeCalculator {

    static final BigDecimal EXEMPT = new BigDecimal("0.00");
    static final BigDecimal BETWEEN_CREDIT_UNIONS_FEE = new BigDecimal("1.50");
    static final BigDecimal EXEMPTION_LIMIT = new BigDecimal("100.00");
    static final BigDecimal OTHER_BANK_PERCENTAGE = new BigDecimal("0.005");
    static final BigDecimal MINIMUM_FEE = new BigDecimal("3.00");

    public BigDecimal calculate(Transfer transfer) {
        return switch (transfer.type()) {
            case INTERNAL -> EXEMPT;
            case BETWEEN_CREDIT_UNIONS -> BETWEEN_CREDIT_UNIONS_FEE;
            case TO_OTHER_BANK -> {
                var withinExemption = transfer.amount().compareTo(EXEMPTION_LIMIT) <= 0;
                yield withinExemption ? EXEMPT : percentageFee(transfer.amount());
            }
        };
    }

    public BigDecimal calculateFee(Transfer transfer) {
        if (transfer instanceof InternalTransfer internal) {
            return EXEMPT;
        } else if (transfer instanceof InterCreditUnionTransfer between) {
            return BETWEEN_CREDIT_UNIONS_FEE;
        } else if (transfer instanceof OtherBankTransfer other) {
            return other.amount().compareTo(EXEMPTION_LIMIT) <= 0 ? EXEMPT : percentageFee(other.amount());
        }
        throw new IllegalStateException("Unhandled transfer type: " + transfer);
    }

    private static BigDecimal percentageFee(BigDecimal amount) {
        return amount.multiply(OTHER_BANK_PERCENTAGE).setScale(2, RoundingMode.HALF_EVEN).max(MINIMUM_FEE);
    }
}
