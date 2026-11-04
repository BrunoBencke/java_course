package cerne.reconciliation;

public record Matched(InternalEntry entry, ExternalRecord externalRecord)
        implements ReconciliationOutcome {
}
