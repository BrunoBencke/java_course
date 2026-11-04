package cerne.transfers.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cerne.members.MemberType;
import cerne.transfers.InMemoryTransferRepository;
import cerne.transfers.InternalTransfer;

class TransferValidatorTest {

    @Test
    @DisplayName("approves when no rule fails")
    void approves() {
        var source = TestContext.account("0001-1", MemberType.INDIVIDUAL, "1000.00");
        var validator = new TransferValidator(ValidatorProducer.defaultRules(
                number -> Optional.of(TestContext.account(number, MemberType.INDIVIDUAL, "0")),
                new InMemoryTransferRepository()));
        assertInstanceOf(ValidationResult.Approved.class,
                validator.validate(TestContext.toOtherBank(source, "100.00")));
    }

    @Test
    @DisplayName("returns all violations at once, in chain order")
    void allViolationsInOrder() {
        var source = TestContext.account("0001-1", MemberType.INDIVIDUAL, "100.00");
        source.block();
        var destination = TestContext.account("0002-3", MemberType.INDIVIDUAL, "0.00");
        destination.block();
        var validator = new TransferValidator(ValidatorProducer.defaultRules(
                number -> Optional.of(destination), new InMemoryTransferRepository()));

        var result = validator.validate(TestContext.of(new InternalTransfer(source, destination, new BigDecimal("500.00"))));

        var rejected = assertInstanceOf(ValidationResult.Rejected.class, result);
        assertEquals(List.of("ACCOUNT_BLOCKED", "DESTINATION_BLOCKED", "INSUFFICIENT_BALANCE"),
                rejected.violations().stream().map(Violation::code).toList());
    }
}
