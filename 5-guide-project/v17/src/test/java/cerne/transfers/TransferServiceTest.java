package cerne.transfers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cerne.accounts.Account;
import cerne.audit.AuditedTransferService;
import cerne.audit.InMemoryAuditLog;

class TransferServiceTest {

    @Nested
    @DisplayName("default service")
    class Default {

        private InMemoryTransferRepository history;
        private Account source;
        private Account destination;
        private TransferService service;

        @BeforeEach
        void setUp() {
            history = new InMemoryTransferRepository();
            source = Scenario.individualAccount("0001-1", "1000.00");
            destination = Scenario.legalEntityAccount("0002-2", "0.00");
            var validator = new TransferValidator(Scenario.repository(source, destination), history,
                    Scenario.MONDAY_10AM);
            service = new DefaultTransferService(validator, new FeeCalculator(), history,
                    Scenario.MONDAY_10AM);
        }

        @Test
        @DisplayName("internal moves both balances and records it in the day's history")
        void internalMovesBothBalances() {
            service.transfer(Scenario.request(new InternalTransfer(source, destination, new BigDecimal("150.00"))));
            assertEquals(new BigDecimal("850.00"), source.balance());
            assertEquals(new BigDecimal("150.00"), destination.balance());
            assertEquals(new BigDecimal("150.00"), history.dailyTotal("0001-1", LocalDate.of(2026, 8, 3)));
        }

        @Test
        @DisplayName("other bank debits amount plus fee")
        void otherBankDebitsAmountPlusFee() {
            var receipt = service.transfer(Scenario.request(
                    new OtherBankTransfer(source, Scenario.EXTERNAL_ACCOUNT, new BigDecimal("500.00"))));
            assertEquals(new BigDecimal("497.00"), source.balance());
            assertEquals(new BigDecimal("3.00"), receipt.fee());
        }

        @Test
        @DisplayName("rejected transfer does not touch the balance")
        void rejectedDoesNotTouchBalance() {
            assertThrows(InvalidTransferException.class, () -> service.transfer(
                    Scenario.request(new InternalTransfer(source, destination, new BigDecimal("5000.00")))));
            assertEquals(new BigDecimal("1000.00"), source.balance());
        }
    }

    @Nested
    @DisplayName("decorators")
    class Decorators {

        private final TransferRequest request = Scenario.request(new InternalTransfer(
                Scenario.individualAccount("0001-1", "100.00"), Scenario.legalEntityAccount("0002-2", "0.00"), BigDecimal.TEN));
        private final TransferReceipt fixedReceipt =
                new TransferReceipt("TRF-FIXED", request, BigDecimal.ZERO, Instant.EPOCH);

        @Test
        @DisplayName("audited records attempt and success")
        void auditedRecordsAttemptAndSuccess() {
            TransferService fake = r -> fixedReceipt;
            var log = new InMemoryAuditLog();
            new AuditedTransferService(fake, log).transfer(request);
            assertEquals(List.of("ATTEMPT", "SUCCESS"), log.events());
        }

        @Test
        @DisplayName("audited records the failure and rethrows")
        void auditedRecordsFailure() {
            TransferService broken = r -> {
                throw new InvalidTransferException("Insufficient balance");
            };
            var log = new InMemoryAuditLog();
            var audited = new AuditedTransferService(broken, log);
            assertThrows(InvalidTransferException.class, () -> audited.transfer(request));
            assertEquals(List.of("ATTEMPT", "FAILURE"), log.events());
        }

        @Test
        @DisplayName("deduplication calls the delegate once for the same key")
        void deduplicationCallsTheDelegateOnce() {
            var calls = new AtomicInteger();
            TransferService standard = r -> {
                calls.incrementAndGet();
                return fixedReceipt;
            };
            var service = new DeduplicatingTransferService(standard, new ConcurrentHashMap<>());
            var first = service.transfer(request);
            var second = service.transfer(request);
            assertEquals(1, calls.get());
            assertSame(first, second);
        }

        @Test
        @DisplayName("audit on the outside sees the duplicated attempt")
        void auditSeesTheDuplicatedAttemptWhenOutside() {
            var log = new InMemoryAuditLog();
            var calls = new AtomicInteger();
            TransferService standard = r -> {
                calls.incrementAndGet();
                return fixedReceipt;
            };

            TransferService service =
                    new AuditedTransferService(
                            new DeduplicatingTransferService(standard, new ConcurrentHashMap<>()),
                            log);

            service.transfer(request);
            service.transfer(request);

            assertEquals(1, calls.get());
            assertEquals(List.of("ATTEMPT", "SUCCESS", "ATTEMPT", "SUCCESS"), log.events());
        }

        @Test
        @DisplayName("audit on the inside does not see the blocked attempt")
        void auditInsideDoesNotSeeTheDuplicate() {
            var log = new InMemoryAuditLog();
            TransferService standard = r -> fixedReceipt;

            TransferService service =
                    new DeduplicatingTransferService(
                            new AuditedTransferService(standard, log),
                            new ConcurrentHashMap<>());

            service.transfer(request);
            service.transfer(request);

            assertEquals(List.of("ATTEMPT", "SUCCESS"), log.events());
        }
    }
}
