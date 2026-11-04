package cerne.transfers.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cerne.accounts.AccountRepository;
import cerne.members.MemberType;
import cerne.transfers.InMemoryTransferRepository;

/** Each rule tested in isolation: record + evaluator, no container and no mocking library. */
class RuleEvaluatorTest {

    private final RuleEvaluator evaluator = new RuleEvaluator();

    @Test
    @DisplayName("active account: rejects a blocked source")
    void activeAccount() {
        var source = TestContext.account("0001-1", MemberType.INDIVIDUAL, "100.00");
        source.block();
        assertEquals(Optional.of(new Violation("ACCOUNT_BLOCKED", "Source account blocked")),
                evaluator.evaluate(new Rule.ActiveAccount(), TestContext.toOtherBank(source, "10.00")));
    }

    @Test
    @DisplayName("active destination: rejects when the destination is blocked")
    void rejectsWhenDestinationBlocked() {
        AccountRepository accounts = number -> {
            var blocked = TestContext.account(number, MemberType.INDIVIDUAL, "0.00");
            blocked.block();
            return Optional.of(blocked);
        };
        assertEquals(Optional.of(new Violation("DESTINATION_BLOCKED", "Destination account blocked or not found")),
                evaluator.evaluate(new Rule.ActiveDestination(accounts), TestContext.withDestination("0002-3")));
    }

    @Test
    @DisplayName("active destination: approves when the destination is active")
    void approvesWhenDestinationActive() {
        AccountRepository accounts = number -> Optional.of(TestContext.account(number, MemberType.INDIVIDUAL, "0"));
        assertTrue(evaluator.evaluate(new Rule.ActiveDestination(accounts), TestContext.withDestination("0002-3")).isEmpty());
    }

    @Test
    @DisplayName("active destination: does not apply to another bank")
    void activeDestinationDoesNotApplyToOtherBank() {
        AccountRepository none = number -> Optional.empty();
        var source = TestContext.account("0001-1", MemberType.INDIVIDUAL, "100.00");
        assertTrue(evaluator.evaluate(new Rule.ActiveDestination(none), TestContext.toOtherBank(source, "10")).isEmpty());
    }

    @Test
    @DisplayName("balance: overdraft only counts for a legal entity")
    void balanceWithOverdraft() {
        var company = TestContext.account("0003-3", MemberType.LEGAL_ENTITY, "100.00");
        company.changeOverdraft(new BigDecimal("1000.00"));
        var person = TestContext.account("0004-4", MemberType.INDIVIDUAL, "100.00");
        person.changeOverdraft(new BigDecimal("1000.00"));
        var rule = new Rule.SufficientBalance(true);
        assertTrue(evaluator.evaluate(rule, TestContext.toOtherBank(company, "900.00")).isEmpty());
        assertEquals("INSUFFICIENT_BALANCE",
                evaluator.evaluate(rule, TestContext.toOtherBank(person, "900.00")).orElseThrow().code());
    }

    @Test
    @DisplayName("daily limit: adds up what already went out that day")
    void dailyLimit() {
        var history = new InMemoryTransferRepository();
        var source = TestContext.account("0001-1", MemberType.INDIVIDUAL, "1000.00");
        source.changeDailyLimit(new BigDecimal("300.00"));
        history.record("0001-1", LocalDate.of(2026, 8, 3), new BigDecimal("250.00"));
        assertEquals("DAILY_LIMIT", evaluator.evaluate(new Rule.DailyLimit(history),
                TestContext.toOtherBank(source, "100.00")).orElseThrow().code());
    }

    @Test
    @DisplayName("rules are data: toString shows the parameters")
    void rulesAreData() {
        assertEquals("SufficientBalance[considerOverdraft=true]", new Rule.SufficientBalance(true).toString());
    }
}
