package cerne.transfers;

import java.math.BigDecimal;
import java.util.Date;

import javax.ejb.Stateless;
import javax.inject.Inject;

import cerne.accounts.Account;
import cerne.audit.AuditService;
import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.EntryRepository;
import cerne.entries.FeeEntry;
import cerne.util.CerneUtil;

/** EJB that executes transfers: validates, charges the fee, moves balances and audits. */
@Stateless
public class TransferService {

    @Inject
    private TransferValidator validator;
    @Inject
    private FeeCalculator calculator;
    @Inject
    private TransferRepository transfers;
    @Inject
    private EntryRepository entries;
    @Inject
    private AuditService audit;

    public TransferService() {
    }

    public TransferService(TransferValidator validator, FeeCalculator calculator,
                           TransferRepository transfers, EntryRepository entries,
                           AuditService audit) {
        this.validator = validator;
        this.calculator = calculator;
        this.transfers = transfers;
        this.entries = entries;
        this.audit = audit;
    }

    public String transfer(Transfer transfer) {
        return transfer(transfer, new Date());
    }

    /** Returns the transfer identifier. */
    public String transfer(Transfer transfer, Date instant) {
        validator.validate(transfer, instant);
        BigDecimal fee = calculator.calculate(transfer);
        Account source = transfer.getSource();
        String identifier = CerneUtil.generateTransferIdentifier();

        source.debit(transfer.getAmount().add(fee));
        DebitEntry debit = new DebitEntry(source.getNumber(),
                transfer.getAmount().doubleValue(), instant);
        debit.setDescription("Transfer " + identifier);
        entries.save(debit);
        if (fee.signum() > 0) {
            FeeEntry feeEntry = new FeeEntry(source.getNumber(),
                    fee.doubleValue(), instant);
            feeEntry.setFeeCode("TRF-" + transfer.getType());
            entries.save(feeEntry);
        }
        if (Transfer.INTERNAL.equals(transfer.getType())) {
            Account destination = transfer.getDestination();
            destination.credit(transfer.getAmount());
            CreditEntry credit = new CreditEntry(destination.getNumber(),
                    transfer.getAmount().doubleValue(), instant);
            credit.setSource(source.getNumber());
            entries.save(credit);
        }
        transfers.record(source.getNumber(), instant, transfer.getAmount());
        audit.record("TRANSFER_COMPLETED", identifier, transfer);
        return identifier;
    }
}
