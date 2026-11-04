package cerne.transfers;

import java.time.Clock;
import java.time.LocalDate;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

/**
 * CDI variant of the daily limit. Without @Priority, CDI 4.0 does not enable it (on purpose: the active one is
 * the manual one). To turn it on: @Priority(Interceptor.Priority.APPLICATION + 10), inner relative to the audit.
 */
@Decorator
public abstract class DailyLimitDecorator implements TransferService {

    @Inject
    @Delegate
    @Any
    TransferService delegate;

    @Inject
    TransferRepository transfers;

    @Inject
    Clock clock;

    @Override
    public TransferReceipt transfer(TransferRequest request) {
        var source = request.transfer().source();
        var dailyTotal = transfers.dailyTotal(source.number(), LocalDate.now(clock));
        if (dailyTotal.add(request.amount()).compareTo(source.dailyLimit()) > 0) {
            throw new DailyLimitExceededException(source.number());
        }
        return delegate.transfer(request);
    }
}
