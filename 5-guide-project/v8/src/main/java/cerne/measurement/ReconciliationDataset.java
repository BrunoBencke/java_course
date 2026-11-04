package cerne.measurement;

import java.util.List;

import cerne.transfers.TransferRecord;

/** Both sides of the reconciliation, generated with the same seed. */
public final class ReconciliationDataset {

    private final List<TransferRecord> fromCerne;
    private final List<TransferRecord> fromFile;

    public ReconciliationDataset(List<TransferRecord> fromCerne, List<TransferRecord> fromFile) {
        this.fromCerne = fromCerne;
        this.fromFile = fromFile;
    }

    public List<TransferRecord> fromCerne() {
        return fromCerne;
    }

    public List<TransferRecord> fromFile() {
        return fromFile;
    }
}
