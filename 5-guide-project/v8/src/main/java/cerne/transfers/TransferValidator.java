package cerne.transfers;

import java.math.BigDecimal;
import java.util.Date;

import javax.inject.Inject;

import cerne.accounts.Account;
import cerne.members.Member;
import cerne.members.MemberType;
import cerne.util.CerneUtil;

/** Validates a transfer before executing it. */
public class TransferValidator {

    private static final BigDecimal INDIVIDUAL_OTHER_BANK_LIMIT = new BigDecimal("5000.00");
    private static final BigDecimal LEGAL_ENTITY_OTHER_BANK_LIMIT = new BigDecimal("50000.00");

    @Inject
    private TransferRepository transfers;

    public TransferValidator() {
    }

    public TransferValidator(TransferRepository transfers) {
        this.transfers = transfers;
    }

    // SMELL 5: ~150 lines of chained ifs; the order is implicit and each rule knows about the others
    public void validate(Transfer t, Date instant) {
        if (t == null) {
            throw new InvalidTransferException("Null transfer");
        }
        if (t.getType() == null) {
            throw new InvalidTransferException("Transfer type not provided");
        }
        if (!Transfer.INTERNAL.equals(t.getType())
                && !Transfer.BETWEEN_CREDIT_UNIONS.equals(t.getType())
                && !Transfer.OTHER_BANK.equals(t.getType())) {
            throw new InvalidTransferException("Unknown transfer type: " + t.getType());
        }
        if (t.getAmount() == null) {
            throw new InvalidTransferException("Amount not provided");
        }
        if (t.getAmount().signum() <= 0) {
            throw new InvalidTransferException("Amount must be positive");
        }
        Account source = t.getSource();
        if (source == null) {
            throw new InvalidTransferException("Source account does not exist");
        }
        if (!source.isActive()) {
            throw new InvalidTransferException("Source account blocked");
        }
        Member holder = source.getHolder();
        if (holder == null) {
            throw new InvalidTransferException("Source account has no holder");
        }
        if (!CerneUtil.isValidCpf(holder.getCpf())) {
            throw new InvalidTransferException("Invalid holder CPF");
        }
        if (Transfer.INTERNAL.equals(t.getType())) {
            if (t.getDestination() == null) {
                throw new InvalidTransferException("Destination account does not exist");
            } else {
                if (!t.getDestination().isActive()) {
                    throw new InvalidTransferException("Destination account blocked");
                }
                if (t.getDestination().getNumber().equals(source.getNumber())) {
                    throw new InvalidTransferException("Source and destination are the same account");
                }
            }
        } else if (Transfer.BETWEEN_CREDIT_UNIONS.equals(t.getType())) {
            if (t.getDestinationCreditUnionCode() == null
                    || t.getDestinationCreditUnionCode().trim().isEmpty()) {
                throw new InvalidTransferException("Destination credit union not provided");
            }
            if (t.getDestinationAccountNumber() == null || t.getDestinationAccountNumber().trim().isEmpty()) {
                throw new InvalidTransferException("Destination account not provided");
            }
        } else {
            if (t.getBankCode() == null || t.getBankCode().trim().isEmpty()) {
                throw new InvalidTransferException("Destination bank not provided");
            } else {
                if (t.getBankCode().trim().length() != 8) {
                    throw new InvalidTransferException("Destination bank ISPB must have 8 digits");
                }
            }
            if (t.getDestinationBranch() == null || t.getDestinationBranch().trim().isEmpty()) {
                throw new InvalidTransferException("Destination branch not provided");
            }
            if (t.getDestinationAccountNumber() == null || t.getDestinationAccountNumber().trim().isEmpty()) {
                throw new InvalidTransferException("Destination account not provided");
            }
            if (!CerneUtil.isBusinessHours(instant)) {
                throw new InvalidTransferException("Transfer to another bank outside business hours");
            }
        }
        if (source.getBalance().compareTo(t.getAmount()) < 0) {
            if (holder.getType() == MemberType.LEGAL_ENTITY
                    && source.getOverdraftLimit() != null
                    && source.getBalance().add(source.getOverdraftLimit()).compareTo(t.getAmount()) >= 0) {
                // allowed: legal entity uses the overdraft
            } else {
                throw new InvalidTransferException("Insufficient balance");
            }
        }
        BigDecimal dailyTotal = transfers.dailyTotal(source.getNumber(), instant);
        if (dailyTotal.add(t.getAmount()).compareTo(source.getDailyLimit()) > 0) {
            throw new InvalidTransferException("Daily limit exceeded");
        }
        if (Transfer.OTHER_BANK.equals(t.getType())) {
            if (holder.getType() == MemberType.INDIVIDUAL
                    || holder.getType() == MemberType.RURAL_PRODUCER) {
                if (t.getAmount().compareTo(INDIVIDUAL_OTHER_BANK_LIMIT) > 0) {
                    throw new InvalidTransferException("Per-transfer limit for individuals exceeded");
                }
            } else if (holder.getType() == MemberType.LEGAL_ENTITY
                    || holder.getType() == MemberType.ASSOCIATION) {
                if (t.getAmount().compareTo(LEGAL_ENTITY_OTHER_BANK_LIMIT) > 0) {
                    throw new InvalidTransferException("Per-transfer limit for legal entities exceeded");
                }
            }
        }
        // three old rules that "cannot be removed"
        if (t.getAmount().scale() > 2) {
            throw new InvalidTransferException("Amount with more than two decimal places");
        }
        if (Transfer.BETWEEN_CREDIT_UNIONS.equals(t.getType())
                && t.getDestinationCreditUnionCode().trim().equals("0000")) {
            throw new InvalidTransferException("Invalid destination credit union");
        }
        if (Transfer.INTERNAL.equals(t.getType())
                && t.getDestination().getHolder() == null) {
            throw new InvalidTransferException("Destination account has no holder");
        }
    }
}
