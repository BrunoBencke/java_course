package cerne.reconciliation;

public record Mismatched(InternalEntry entry, ExternalRecord externalRecord)
        implements ReconciliationOutcome {
}
