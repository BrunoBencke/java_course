package cerne.audit;

import cerne.transfers.TransferReceipt;
import cerne.transfers.TransferRequest;

/** Where the audit trail is written. */
public interface AuditLog {

    void recordAttempt(TransferRequest request);

    void recordSuccess(TransferReceipt receipt);

    void recordFailure(TransferRequest request, RuntimeException error);
}
