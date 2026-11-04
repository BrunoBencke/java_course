package cerne.reconciliation;

import java.math.BigDecimal;

/** Decides the severity of each outcome for the handling queue. */
public class DiscrepancyClassifier {

    private static final BigDecimal HIGH_DIFFERENCE = new BigDecimal("100.00");

    public DiscrepancySeverity classify(ReconciliationOutcome outcome) {
        if (outcome instanceof Matched) {
            return DiscrepancySeverity.NONE;
        }
        if (outcome instanceof Pending p) {
            return p.daysOpen() > 5 ? DiscrepancySeverity.HIGH : DiscrepancySeverity.MEDIUM;
        }
        if (outcome instanceof Mismatched m) {
            BigDecimal difference = m.entry().amount().subtract(m.externalRecord().amount()).abs();
            if (difference.compareTo(ReconciliationReportGenerator.TOLERANCE) <= 0) {
                return DiscrepancySeverity.LOW;
            }
            return difference.compareTo(HIGH_DIFFERENCE) > 0 ? DiscrepancySeverity.HIGH : DiscrepancySeverity.MEDIUM;
        }
        throw new IllegalStateException("Unhandled outcome: " + outcome);
    }
}
