package cerne.transfers.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cerne.members.MemberType;
import cerne.transfers.ExternalAccount;
import cerne.transfers.InMemoryTransferRepository;
import cerne.transfers.OtherBankTransfer;
import cerne.transfers.TransferRequest;

class ValidatorWithChallengeRulesTest {

    @Test
    @DisplayName("hours, balance and limit per type violated: three violations, in chain order")
    void threeViolationsInChainOrder() {
        var source = TestContext.account("0001-1", MemberType.INDIVIDUAL, "100.00");
        source.changeDailyLimit(new BigDecimal("100000.00"));
        var transfer = new OtherBankTransfer(source, new ExternalAccount("00000208", "1234", "9"),
                new BigDecimal("6000.00"));
        var saturday = LocalDateTime.of(2026, 8, 8, 10, 0);
        var context = new ValidationContext(TransferRequest.of(transfer), source, saturday);
        var validator = new TransferValidator(ValidatorProducer.defaultRules(
                number -> Optional.empty(), new InMemoryTransferRepository(), new DefaultLimitPolicy()));

        var rejected = assertInstanceOf(ValidationResult.Rejected.class, validator.validate(context));

        assertEquals(List.of("OUTSIDE_ALLOWED_HOURS", "INSUFFICIENT_BALANCE", "LIMIT_PER_MEMBER_TYPE"),
                rejected.violations().stream().map(Violation::code).toList());
    }

    @Test
    @DisplayName("the rule that failed is identifiable: the record carries the name and the parameters")
    void ruleIsIdentifiable() {
        assertEquals("AllowedHours[betweenCreditUnions=TimeWindow[start=06:00, end=22:00, businessDaysOnly=false], "
                        + "toOtherBank=TimeWindow[start=06:30, end=17:00, businessDaysOnly=true]]",
                Rule.AllowedHours.defaults().toString());
    }
}
