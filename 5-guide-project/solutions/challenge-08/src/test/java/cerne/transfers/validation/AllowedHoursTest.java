package cerne.transfers.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cerne.members.MemberType;
import cerne.transfers.CreditUnionAccount;
import cerne.transfers.ExternalAccount;
import cerne.transfers.InterCreditUnionTransfer;
import cerne.transfers.InternalTransfer;
import cerne.transfers.OtherBankTransfer;
import cerne.transfers.Transfer;
import cerne.transfers.TransferRequest;

class AllowedHoursTest {

    private static final ZoneId TIME_ZONE = ZoneId.of("America/Sao_Paulo");
    private final RuleEvaluator evaluator = new RuleEvaluator();

    /** The context's now comes from a fixed Clock, as in the service. */
    private static ValidationContext at(String instant, String type) {
        var clock = Clock.fixed(LocalDateTime.parse(instant).atZone(TIME_ZONE).toInstant(), TIME_ZONE);
        var source = TestContext.account("0001-1", MemberType.INDIVIDUAL, "1000.00");
        Transfer transfer = switch (type) {
            case "INTERNAL" -> new InternalTransfer(source,
                    TestContext.account("0002-2", MemberType.INDIVIDUAL, "0"), BigDecimal.TEN);
            case "BETWEEN_CREDIT_UNIONS" -> new InterCreditUnionTransfer(source, new CreditUnionAccount("0102", "1"),
                    BigDecimal.TEN);
            default -> new OtherBankTransfer(source, new ExternalAccount("00000208", "1234", "9"), BigDecimal.TEN);
        };
        return new ValidationContext(TransferRequest.of(transfer), source, LocalDateTime.now(clock));
    }

    @ParameterizedTest(name = "{1} at {0}")
    @CsvSource({
            "2026-08-03T03:00, INTERNAL",
            "2026-08-08T23:30, INTERNAL",
            "2026-08-03T06:00, BETWEEN_CREDIT_UNIONS",
            "2026-08-03T21:59, BETWEEN_CREDIT_UNIONS",
            "2026-08-03T06:30, TO_OTHER_BANK",
            "2026-08-03T16:59, TO_OTHER_BANK"
    })
    @DisplayName("approves inside the window, including at the opening edges")
    void approvesInsideTheWindow(String instant, String type) {
        assertTrue(evaluator.evaluate(Rule.AllowedHours.defaults(), at(instant, type)).isEmpty());
    }

    @ParameterizedTest(name = "{1} at {0}")
    @CsvSource({
            "2026-08-03T05:59, BETWEEN_CREDIT_UNIONS",
            "2026-08-03T22:00, BETWEEN_CREDIT_UNIONS",
            "2026-08-03T06:29, TO_OTHER_BANK",
            "2026-08-03T17:00, TO_OTHER_BANK",
            "2026-08-08T10:00, TO_OTHER_BANK"
    })
    @DisplayName("rejects outside the window, at closing and on Saturday for another bank")
    void rejectsOutsideTheWindow(String instant, String type) {
        var violation = evaluator.evaluate(Rule.AllowedHours.defaults(), at(instant, type)).orElseThrow();
        assertEquals("OUTSIDE_ALLOWED_HOURS", violation.code());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"TO_OTHER_BANK, 'Transfer outside the allowed window: 06:30–17:00 on business days'"})
    @DisplayName("the message states the valid window")
    void messageStatesTheWindow(String type, String message) {
        assertEquals(message,
                evaluator.evaluate(Rule.AllowedHours.defaults(), at("2026-08-03T20:00", type)).orElseThrow().message());
    }
}
