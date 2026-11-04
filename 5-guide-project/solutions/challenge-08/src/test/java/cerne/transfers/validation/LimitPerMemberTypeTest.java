package cerne.transfers.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cerne.accounts.Account;
import cerne.members.Member;
import cerne.members.MemberType;

class LimitPerMemberTypeTest {

    private final RuleEvaluator evaluator = new RuleEvaluator();
    private final Rule rule = new Rule.LimitPerMemberType(new DefaultLimitPolicy());

    private static Account account(MemberType type, boolean agreement) {
        var holder = new Member("52998224725", "Holder", type, agreement);
        var account = new Account("0001-1", TestContext.BRANCH, holder, new BigDecimal("100000.00"));
        holder.addAccount(account);
        return account;
    }

    @ParameterizedTest(name = "{0} with {1}")
    @CsvSource({"INDIVIDUAL, 5000.00", "RURAL_PRODUCER, 5000.00", "LEGAL_ENTITY, 50000.00", "ASSOCIATION, 50000.00"})
    @DisplayName("approves exactly at the limit (edge)")
    void approvesAtTheLimit(MemberType type, String amount) {
        assertTrue(evaluator.evaluate(rule, TestContext.toOtherBank(account(type, false), amount)).isEmpty());
    }

    @ParameterizedTest(name = "{0} with {1}")
    @CsvSource({"INDIVIDUAL, 5000.01", "LEGAL_ENTITY, 50000.01"})
    @DisplayName("rejects one cent above the limit")
    void rejectsAboveTheLimit(MemberType type, String amount) {
        var violation = evaluator.evaluate(rule, TestContext.toOtherBank(account(type, false), amount)).orElseThrow();
        assertEquals("LIMIT_PER_MEMBER_TYPE", violation.code());
    }

    @Test
    @DisplayName("a member with a special agreement has no limit")
    void specialAgreementHasNoLimit() {
        assertTrue(evaluator.evaluate(rule,
                TestContext.toOtherBank(account(MemberType.INDIVIDUAL, true), "90000.00")).isEmpty());
    }

    @Test
    @DisplayName("does not apply to an internal transfer")
    void doesNotApplyToInternal() {
        var context = TestContext.withDestination("0002-3");
        assertTrue(evaluator.evaluate(new Rule.LimitPerMemberType(member -> java.util.Optional.of(BigDecimal.ONE)),
                context).isEmpty());
    }
}
