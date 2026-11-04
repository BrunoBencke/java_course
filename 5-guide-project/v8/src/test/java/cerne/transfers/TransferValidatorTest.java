package cerne.transfers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import cerne.accounts.Account;
import cerne.members.MemberType;

public class TransferValidatorTest {

    private InMemoryTransferRepository history;
    private TransferValidator validator;
    private Account source;
    private Account destination;

    @Before
    public void setUp() {
        history = new InMemoryTransferRepository();
        validator = new TransferValidator(history);
        source = Scenario.individualAccount("0001-1", "1000.00");
        destination = Scenario.account("0002-2", "11144477735", MemberType.INDIVIDUAL, "0.00");
    }

    private void assertRejects(Transfer t, String expectedMessage) {
        try {
            validator.validate(t, Scenario.BUSINESS_DAY_10AM);
            fail("should have rejected: " + expectedMessage);
        } catch (InvalidTransferException e) {
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    @Test
    public void acceptsValidInternalTransfer() {
        validator.validate(Transfer.internal(source, destination, new BigDecimal("100.00")), Scenario.BUSINESS_DAY_10AM);
    }

    @Test
    public void rejectsZeroAmount() {
        assertRejects(Transfer.internal(source, destination, BigDecimal.ZERO), "Amount must be positive");
    }

    @Test
    public void rejectsBlockedSourceAccount() {
        source.setActive(false);
        assertRejects(Transfer.internal(source, destination, BigDecimal.TEN), "Source account blocked");
    }

    @Test
    public void rejectsBlockedDestination() {
        destination.setActive(false);
        assertRejects(Transfer.internal(source, destination, BigDecimal.TEN), "Destination account blocked");
    }

    @Test
    public void rejectsInsufficientBalance() {
        assertRejects(Transfer.internal(source, destination, new BigDecimal("1500.00")), "Insufficient balance");
    }

    @Test
    public void legalEntityUsesOverdraft() {
        Account company = Scenario.account("0003-3", "11144477735", MemberType.LEGAL_ENTITY, "100.00");
        company.setOverdraftLimit(new BigDecimal("1000.00"));
        validator.validate(Transfer.internal(company, destination, new BigDecimal("900.00")), Scenario.BUSINESS_DAY_10AM);
    }

    @Test
    public void rejectsDailyLimitExceeded() {
        source.setDailyLimit(new BigDecimal("300.00"));
        history.record(source.getNumber(), Scenario.BUSINESS_DAY_10AM, new BigDecimal("250.00"));
        assertRejects(Transfer.internal(source, destination, new BigDecimal("100.00")), "Daily limit exceeded");
    }

    @Test
    public void rejectsOtherBankOutsideBusinessHours() {
        try {
            validator.validate(Transfer.toOtherBank(source, "00000208", "1234", "99887-6", BigDecimal.TEN),
                    Scenario.BUSINESS_DAY_8PM);
            fail("should have rejected outside business hours");
        } catch (InvalidTransferException e) {
            assertEquals("Transfer to another bank outside business hours", e.getMessage());
        }
    }

    @Test
    public void rejectsIspbWithWrongLength() {
        assertRejects(Transfer.toOtherBank(source, "208", "1234", "99887-6", BigDecimal.TEN),
                "Destination bank ISPB must have 8 digits");
    }
}
