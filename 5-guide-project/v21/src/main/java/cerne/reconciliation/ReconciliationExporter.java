package cerne.reconciliation;

/** Exports the outcomes as CSV for the other credit union: identifier;status;internal amount;external amount. */
public class ReconciliationExporter {

    public String csvLine(ReconciliationOutcome outcome) {
        if (outcome instanceof Matched m) {
            return String.join(";", m.entry().identifier(), "MATCHED",
                    m.entry().amount().toPlainString(), m.externalRecord().amount().toPlainString());
        } else if (outcome instanceof Pending p) {
            return String.join(";", p.entry().identifier(), "PENDING",
                    p.entry().amount().toPlainString(), "");
        } else if (outcome instanceof Mismatched m) {
            return String.join(";", m.entry().identifier(), "MISMATCHED",
                    m.entry().amount().toPlainString(), m.externalRecord().amount().toPlainString());
        }
        throw new IllegalStateException("Unhandled outcome: " + outcome);
    }
}
