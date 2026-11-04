package cerne.transfers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cerne.accounts.Account;

class TransferValidatorTest {

    private InMemoryTransferRepository history;
    private Account source;
    private Account destination;

    @BeforeEach
    void setUp() {
        history = new InMemoryTransferRepository();
        source = Scenario.individualAccount("0001-1", "1000.00");
        destination = Scenario.legalEntityAccount("0002-2", "0.00");
    }

    private TransferValidator validator(Clock clock, Account... accounts) {
        return new TransferValidator(Scenario.repository(accounts), history, clock);
    }

    private void assertRejects(Transfer t, String message) {
        var error = assertThrows(InvalidTransferException.class,
                () -> validator(Scenario.MONDAY_10AM, source, destination).validate(Scenario.request(t)));
        assertEquals(message, error.getMessage());
    }

    @Test
    @DisplayName("accepts a valid internal transfer")
    void acceptsValidInternal() {
        assertDoesNotThrow(() -> validator(Scenario.MONDAY_10AM, source, destination)
                .validate(Scenario.request(new InternalTransfer(source, destination, new BigDecimal("100.00")))));
    }

    @Test
    void rejectsBlockedSource() {
        source.block();
        assertRejects(new InternalTransfer(source, destination, BigDecimal.TEN), "Source account blocked");
    }

    @Test
    void rejectsBlockedDestination() {
        destination.block();
        assertRejects(new InternalTransfer(source, destination, BigDecimal.TEN), "Destination account blocked");
    }

    @Test
    void rejectsInsufficientBalance() {
        assertRejects(new InternalTransfer(source, destination, new BigDecimal("1500.00")), "Insufficient balance");
    }

    @Test
    @DisplayName("overdraft applies only to legal entities")
    void overdraftOnlyForLegalEntities() {
        var company = Scenario.legalEntityAccount("0003-3", "100.00");
        company.changeOverdraft(new BigDecimal("1000.00"));
        source.changeOverdraft(new BigDecimal("1000.00"));
        var validator = validator(Scenario.MONDAY_10AM, source, destination, company);
        assertDoesNotThrow(() -> validator.validate(Scenario.request(
                new InternalTransfer(company, destination, new BigDecimal("900.00")))));
        assertThrows(InvalidTransferException.class, () -> validator.validate(Scenario.request(
                new InternalTransfer(source, destination, new BigDecimal("1500.00")))));
    }

    @Test
    void rejectsDailyLimitExceeded() {
        source.changeDailyLimit(new BigDecimal("300.00"));
        history.record("0001-1", LocalDate.of(2026, 8, 3), new BigDecimal("250.00"));
        assertRejects(new InternalTransfer(source, destination, new BigDecimal("100.00")), "Daily limit exceeded");
    }

    @Test
    void rejectsOtherBankOutsideBusinessHours() {
        var error = assertThrows(InvalidTransferException.class,
                () -> validator(Scenario.MONDAY_8PM, source).validate(Scenario.request(
                        new OtherBankTransfer(source, Scenario.EXTERNAL_ACCOUNT, BigDecimal.TEN))));
        assertEquals("Transfer to another bank outside business hours", error.getMessage());
    }

    @Test
    @DisplayName("individuals have a per-transfer limit for other banks; a special agreement does not")
    void limitPerMemberType() {
        source.changeDailyLimit(new BigDecimal("100000.00"));
        var rich = Scenario.accountWithAgreement("0004-4", "100000.00");
        rich.changeDailyLimit(new BigDecimal("100000.00"));
        source.credit(new BigDecimal("9000.00"));
        var validator = validator(Scenario.MONDAY_10AM, source, rich);
        var error = assertThrows(InvalidTransferException.class, () -> validator.validate(Scenario.request(
                new OtherBankTransfer(source, Scenario.EXTERNAL_ACCOUNT, new BigDecimal("6000.00")))));
        assertEquals("Per-transfer limit for individuals exceeded", error.getMessage());
        assertDoesNotThrow(() -> validator.validate(Scenario.request(
                new OtherBankTransfer(rich, Scenario.EXTERNAL_ACCOUNT, new BigDecimal("6000.00")))));
    }
}
