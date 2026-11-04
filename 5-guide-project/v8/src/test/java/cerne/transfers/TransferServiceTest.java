package cerne.transfers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import cerne.accounts.Account;
import cerne.audit.AuditService;
import cerne.entries.Entry;
import cerne.entries.InMemoryEntryRepository;
import cerne.members.MemberType;
import cerne.util.CerneUtil;

public class TransferServiceTest {

    private InMemoryEntryRepository entries;
    private AuditService audit;
    private TransferService service;
    private Account source;
    private Account destination;

    @Before
    public void setUp() {
        InMemoryTransferRepository history = new InMemoryTransferRepository();
        entries = new InMemoryEntryRepository();
        audit = new AuditService();
        service = new TransferService(new TransferValidator(history), new FeeCalculator(),
                history, entries, audit);
        source = Scenario.individualAccount("0001-1", "1000.00");
        destination = Scenario.account("0002-2", "11144477735", MemberType.INDIVIDUAL, "0.00");
    }

    @Test
    public void internalTransferMovesBothBalances() {
        service.transfer(Transfer.internal(source, destination, new BigDecimal("150.00")), Scenario.BUSINESS_DAY_10AM);
        assertEquals(new BigDecimal("850.00"), source.getBalance());
        assertEquals(new BigDecimal("150.00"), destination.getBalance());
    }

    @Test
    public void otherBankTransferDebitsAmountPlusFee() {
        service.transfer(Transfer.toOtherBank(source, "00000208", "1234", "99887-6", new BigDecimal("500.00")),
                Scenario.BUSINESS_DAY_10AM);
        assertEquals(new BigDecimal("497.00"), source.getBalance());
        List<Entry> ofSource = entries.findByAccount(source.getNumber(),
                CerneUtil.date(2026, 8, 1), CerneUtil.date(2026, 8, 31));
        assertEquals(2, ofSource.size());
    }

    @Test
    public void completedTransferGoesToAuditTrail() {
        String identifier = service.transfer(Transfer.internal(source, destination, BigDecimal.TEN),
                Scenario.BUSINESS_DAY_10AM);
        assertEquals(1, audit.getRecords().size());
        assertTrue(audit.getRecords().get(0).contains("<identifier>" + identifier + "</identifier>"));
    }

    @Test
    public void rejectedTransferDoesNotTouchBalance() {
        try {
            service.transfer(Transfer.internal(source, destination, new BigDecimal("5000.00")), Scenario.BUSINESS_DAY_10AM);
            fail("should have rejected");
        } catch (InvalidTransferException e) {
            assertEquals(new BigDecimal("1000.00"), source.getBalance());
            assertTrue(audit.getRecords().isEmpty());
        }
    }
}
