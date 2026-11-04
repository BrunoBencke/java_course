package cerne.transfers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import cerne.accounts.AccountRepository;
import cerne.transfers.validation.TransferValidator;
import cerne.transfers.validation.ValidationContext;
import cerne.transfers.validation.ValidationResult;
import cerne.transfers.validation.Violation;

/** The real service: validates through the rule chain, charges the fee and moves balances. */
@ApplicationScoped
public class DefaultTransferService implements TransferService {

    private AccountRepository accounts;
    private TransferValidator validator;
    private FeeCalculator calculator;
    private TransferRepository transfers;
    private Clock clock;

    protected DefaultTransferService() {
    }

    @Inject
    public DefaultTransferService(AccountRepository accounts, TransferValidator validator,
                                  FeeCalculator calculator, TransferRepository transfers,
                                  Clock clock) {
        this.accounts = accounts;
        this.validator = validator;
        this.calculator = calculator;
        this.transfers = transfers;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TransferReceipt transfer(TransferRequest request) {
        var source = accounts.findByNumber(request.sourceAccount())
                .orElseThrow(() -> new TransferRejectedException(
                        List.of(new Violation("ACCOUNT_NOT_FOUND", "Source account not found"))));
        var context = new ValidationContext(request, source, LocalDateTime.now(clock));
        switch (validator.validate(context)) {
            case ValidationResult.Rejected(var violations) -> throw new TransferRejectedException(violations);
            case ValidationResult.Approved approved -> { }
        }
        BigDecimal fee = calculator.calculate(request.transfer());
        source.debit(request.amount().add(fee));
        switch (request.transfer()) {
            case InternalTransfer(var from, var destination, var amount) -> destination.credit(amount);
            case InterCreditUnionTransfer between -> { } // the credit goes out through the central
            case OtherBankTransfer other -> { }          // the credit goes out through the payment system
        }
        transfers.record(source.number(), LocalDate.now(clock), request.amount());
        return new TransferReceipt(identifier(), request, fee, Instant.now(clock));
    }

    private static String identifier() {
        return "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
