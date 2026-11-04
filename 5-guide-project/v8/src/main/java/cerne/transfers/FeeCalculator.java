package cerne.transfers;

import java.math.BigDecimal;

/** Fee per transfer type (switch on String). */
public class FeeCalculator {

    static final BigDecimal EXEMPT = new BigDecimal("0.00");
    static final BigDecimal BETWEEN_CREDIT_UNIONS_FEE = new BigDecimal("1.50");
    static final BigDecimal EXEMPTION_LIMIT = new BigDecimal("100.00");
    static final BigDecimal OTHER_BANK_PERCENTAGE = new BigDecimal("0.005");
    static final BigDecimal MINIMUM_FEE = new BigDecimal("3.00");

    public BigDecimal calculate(Transfer transfer) {
        BigDecimal fee;
        switch (transfer.getType()) {
            case Transfer.INTERNAL:
                fee = EXEMPT;
                break;
            case Transfer.BETWEEN_CREDIT_UNIONS:
                fee = BETWEEN_CREDIT_UNIONS_FEE;
                break;
            case Transfer.OTHER_BANK:
                if (transfer.getAmount().compareTo(EXEMPTION_LIMIT) <= 0) {
                    fee = EXEMPT;
                } else {
                    fee = transfer.getAmount().multiply(OTHER_BANK_PERCENTAGE)
                            .setScale(2, BigDecimal.ROUND_HALF_EVEN).max(MINIMUM_FEE);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown transfer type: " + transfer.getType());
        }
        return fee;
    }
}
