package cerne.measurement;

import java.util.List;

import cerne.transfers.TransferRecord;

/** Both sides of the reconciliation generated with the same seed. */
public record ReconciliationDataset(List<TransferRecord> fromCerne, List<TransferRecord> fromFile) {
}
