package cerne.reconciliation;

public record Pending(InternalEntry entry, int daysOpen) implements ReconciliationOutcome {
}
