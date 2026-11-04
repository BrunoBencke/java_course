package cerne.transfers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import cerne.accounts.Account;
import cerne.accounts.AccountRepository;
import cerne.members.Cpf;
import cerne.members.Member;
import cerne.members.MemberType;

/**
 * Still the chained-ifs method (target of session 8): the order is implicit, the first failure stops it
 * and each rule knows about the others. The records already guarantee a positive amount and a filled-in
 * destination.
 */
@ApplicationScoped
public class TransferValidator {

    private static final BigDecimal INDIVIDUAL_OTHER_BANK_LIMIT = new BigDecimal("5000.00");
    private static final BigDecimal LEGAL_ENTITY_OTHER_BANK_LIMIT = new BigDecimal("50000.00");
    private static final LocalTime OPENING = LocalTime.of(8, 0);
    private static final LocalTime CLOSING = LocalTime.of(18, 0);

    private AccountRepository accounts;
    private TransferRepository transfers;
    private Clock clock;

    protected TransferValidator() {
    }

    @Inject
    public TransferValidator(AccountRepository accounts, TransferRepository transfers,
                             Clock clock) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.clock = clock;
    }

    public void validate(TransferRequest r) {
        if (r == null) {
            throw new InvalidTransferException("Null request");
        }
        if (r.amount().signum() <= 0) {
            throw new InvalidTransferException("Amount must be positive");
        }
        Account source = accounts.findByNumber(r.sourceAccount())
                .orElseThrow(() -> new InvalidTransferException("Source account does not exist"));
        if (!source.active()) {
            throw new InvalidTransferException("Source account blocked");
        }
        Member holder = source.holder();
        if (holder == null) {
            throw new InvalidTransferException("Source account has no holder");
        }
        if (!Cpf.isValid(holder.getCpf())) {
            throw new InvalidTransferException("Invalid holder CPF");
        }
        TransferType type = r.transfer().type();
        if (type == TransferType.INTERNAL) {
            Account destination = accounts.findByNumber(r.destinationAccount()).orElse(null);
            if (destination == null) {
                throw new InvalidTransferException("Destination account does not exist");
            } else {
                if (!destination.active()) {
                    throw new InvalidTransferException("Destination account blocked");
                }
                if (destination.number().equals(source.number())) {
                    throw new InvalidTransferException("Source and destination are the same account");
                }
            }
        } else if (type == TransferType.BETWEEN_CREDIT_UNIONS) {
            if (r.destinationAccount().equals(source.number())) {
                throw new InvalidTransferException("Invalid destination account");
            }
        } else {
            LocalTime now = LocalTime.now(clock);
            if (now.isBefore(OPENING) || !now.isBefore(CLOSING)) {
                throw new InvalidTransferException("Transfer to another bank outside business hours");
            }
        }
        if (source.balance().compareTo(r.amount()) < 0) {
            if (holder.getType() == MemberType.LEGAL_ENTITY
                    && source.overdraftLimit().isPresent()
                    && source.balance().add(source.overdraftLimit().get()).compareTo(r.amount()) >= 0) {
                // allowed: a legal entity uses the overdraft
            } else {
                throw new InvalidTransferException("Insufficient balance");
            }
        }
        BigDecimal dailyTotal = transfers.dailyTotal(source.number(), LocalDate.now(clock));
        if (dailyTotal.add(r.amount()).compareTo(source.dailyLimit()) > 0) {
            throw new InvalidTransferException("Daily limit exceeded");
        }
        if (type == TransferType.TO_OTHER_BANK && !holder.hasSpecialAgreement()) {
            if (holder.getType() == MemberType.INDIVIDUAL
                    || holder.getType() == MemberType.RURAL_PRODUCER) {
                if (r.amount().compareTo(INDIVIDUAL_OTHER_BANK_LIMIT) > 0) {
                    throw new InvalidTransferException("Per-transfer limit for individuals exceeded");
                }
            } else if (holder.getType() == MemberType.LEGAL_ENTITY
                    || holder.getType() == MemberType.ASSOCIATION) {
                if (r.amount().compareTo(LEGAL_ENTITY_OTHER_BANK_LIMIT) > 0) {
                    throw new InvalidTransferException("Per-transfer limit for legal entities exceeded");
                }
            }
        }
    }
}
