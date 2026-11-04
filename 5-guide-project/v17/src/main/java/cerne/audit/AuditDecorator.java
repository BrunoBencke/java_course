package cerne.audit;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import cerne.transfers.TransferReceipt;
import cerne.transfers.TransferRequest;
import cerne.transfers.TransferService;

/**
 * CDI variant of the same Decorator: it applies to every injection point, but the stack is not written
 * down anywhere. Without @Priority, in CDI 4.0 it simply does not run. It only compiles here: there is no
 * container in the project.
 */
@Decorator
@Priority(Interceptor.Priority.APPLICATION)
public abstract class AuditDecorator implements TransferService {

    @Inject
    @Delegate
    @Any
    TransferService delegate;

    @Inject
    AuditLog audit;

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
