package cerne.transfers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import cerne.accounts.Account;

/** The real service: validates, charges the fee and moves balances. Audit and deduplication come in as decorators. */
@ApplicationScoped
public class DefaultTransferService implements TransferService {

    private TransferValidator validator;
    private FeeCalculator calculator;
    private TransferRepository transfers;
    private Clock clock;

    protected DefaultTransferService() {
    }

    @Inject
    public DefaultTransferService(TransferValidator validator,
                                  FeeCalculator calculator, TransferRepository transfers,
                                  Clock clock) {
        this.validator = validator;
        this.calculator = calculator;
        this.transfers = transfers;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TransferReceipt transfer(TransferRequest request) {
        validator.validate(request);
        BigDecimal fee = calculator.calculate(request.transfer());
        Account source = request.transfer().source();
        source.debit(request.amount().add(fee));
        if (request.transfer() instanceof InternalTransfer internal) {
            internal.destination().credit(internal.amount());
        }
        transfers.record(source.number(), LocalDate.now(clock), request.amount());
        return new TransferReceipt(identifier(), request, fee, Instant.now(clock));
    }

    private static String identifier() {
        return "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
