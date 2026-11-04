package cerne.transfers;

import java.util.Map;

/**
 * Decorator (session 7): transferring is not idempotent, so this is not a result cache, it is resend
 * detection. The same key returns the same receipt instead of debiting again.
 */
public class DeduplicatingTransferService implements TransferService {

    private final TransferService delegate;
    private final Map<IdempotencyKey, TransferReceipt> recent;

    public DeduplicatingTransferService(TransferService delegate,
                                        Map<IdempotencyKey, TransferReceipt> recent) {
        this.delegate = delegate;
        this.recent = recent;
    }

    @Override
    public TransferReceipt transfer(TransferRequest request) {
        return recent.computeIfAbsent(request.idempotencyKey(), key -> delegate.transfer(request));
    }
}
