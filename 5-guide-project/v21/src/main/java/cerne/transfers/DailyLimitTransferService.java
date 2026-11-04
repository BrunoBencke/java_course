package cerne.transfers;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Solution to the session 7 challenge: daily limit Decorator, manual version.
 * Active: this one, assembled in TransferServiceComposition, because the order stays written down and testable
 * without a container. The CDI variant (DailyLimitDecorator) has no @Priority, so it is switched off.
 * After session 8 the DailyLimit rule of the validation chain covers the same case; the decorator stays
 * as an example of the pattern.
 */
public class DailyLimitTransferService implements TransferService {

    private final TransferService delegate;
    private final TransferRepository transfers;
    private final Clock clock;

    public DailyLimitTransferService(TransferService delegate,
                                     TransferRepository transfers, Clock clock) {
        this.delegate = delegate;
        this.transfers = transfers;
        this.clock = clock;
    }

    @Override
    public TransferReceipt transfer(TransferRequest request) {
        var source = request.transfer().source();
        var dailyTotal = transfers.dailyTotal(source.number(), LocalDate.now(clock));
        if (dailyTotal.add(request.amount()).compareTo(source.dailyLimit()) > 0) {
            throw new DailyLimitExceededException(source.number()); // the delegate is not even called
        }
        return delegate.transfer(request);
    }
}
