package cerne.audit;

import cerne.transfers.TransferReceipt;
import cerne.transfers.TransferRequest;
import cerne.transfers.TransferService;

/** Manual Decorator (session 7): same interface, delegate received from outside, audit before and after. */
public class AuditedTransferService implements TransferService {

    private final TransferService delegate;
    private final AuditLog audit;

    public AuditedTransferService(TransferService delegate, AuditLog audit) {
        this.delegate = delegate;
        this.audit = audit;
    }

    @Override
    public TransferReceipt transfer(TransferRequest request) {
        audit.recordAttempt(request);
        try {
            var receipt = delegate.transfer(request);
            audit.recordSuccess(receipt);
            return receipt;
        } catch (RuntimeException e) {
            audit.recordFailure(request, e);
            throw e;
        }
    }
}
