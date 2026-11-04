package cerne.transfers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

import cerne.members.MemberType;

/**
 * Fee with a switch over the sealed hierarchy (Java 21): exhaustive, no default and no "unhandled type"
 * throw. A fourth type in the permits makes this method stop compiling.
 */
public class FeeCalculator {

    static final BigDecimal EXEMPT = new BigDecimal("0.00");
    static final BigDecimal BETWEEN_CREDIT_UNIONS_FEE = new BigDecimal("1.50");
    static final BigDecimal EXEMPTION_LIMIT = new BigDecimal("100.00");
    static final BigDecimal OTHER_BANK_PERCENTAGE = new BigDecimal("0.005");
    static final BigDecimal MINIMUM_FEE = new BigDecimal("3.00");
    /** ISPB of cooperative banks: a rural producer does not pay to transfer to them. */
    static final Set<String> COOPERATIVE_BANKS = Set.of("02038232", "01181521");

    public BigDecimal calculate(Transfer transfer) {
        return switch (transfer) {
            // nested record pattern + guard: comes before the general case of the same type (dominance)
            case OtherBankTransfer(var source, ExternalAccount(var ispb, var branch, var number), var amount)
                    when source.holderType() == MemberType.RURAL_PRODUCER
                            && COOPERATIVE_BANKS.contains(ispb) -> EXEMPT;
            case OtherBankTransfer other when other.amount().compareTo(EXEMPTION_LIMIT) <= 0 -> EXEMPT;
            case OtherBankTransfer other -> percentageFee(other.amount());
            case InternalTransfer internal -> EXEMPT;
            case InterCreditUnionTransfer between -> BETWEEN_CREDIT_UNIONS_FEE;
        };
    }

    private static BigDecimal percentageFee(BigDecimal amount) {
        return amount.multiply(OTHER_BANK_PERCENTAGE).setScale(2, RoundingMode.HALF_EVEN).max(MINIMUM_FEE);
    }
}
