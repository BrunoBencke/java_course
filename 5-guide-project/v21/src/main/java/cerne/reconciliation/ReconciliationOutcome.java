package cerne.reconciliation;

/** Outcome of reconciling one entry: closed hierarchy. */
public sealed interface ReconciliationOutcome permits Matched, Pending, Mismatched {
}
