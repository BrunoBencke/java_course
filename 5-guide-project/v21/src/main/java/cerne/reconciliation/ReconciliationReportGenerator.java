package cerne.reconciliation;

import java.math.BigDecimal;

/** One line of text per outcome, for the reconciliation team. */
public class ReconciliationReportGenerator {

    static final BigDecimal TOLERANCE = new BigDecimal("0.05");

    public String describe(ReconciliationOutcome outcome) {
        if (outcome instanceof Matched m) {
            return "OK " + m.entry().identifier();
        } else if (outcome instanceof Pending p) {
            return "PENDING " + p.entry().identifier() + " for " + p.daysOpen() + " days";
        } else if (outcome instanceof Mismatched m) {
            BigDecimal difference = m.entry().amount().subtract(m.externalRecord().amount());
            if (difference.abs().compareTo(TOLERANCE) <= 0) {
                return "ROUNDING " + m.entry().identifier();
            }
            return "MISMATCHED " + m.entry().identifier() + " difference " + difference;
        }
        throw new IllegalStateException("Unhandled outcome: " + outcome);
    }
}
