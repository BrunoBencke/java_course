package cerne.transfers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cerne.members.Member;
import cerne.members.MemberType;

class FeeCalculatorTest {

    private final FeeCalculator calculator = new FeeCalculator();

    private static Transfer transfer(String type, String amount) {
        var source = Scenario.individualAccount("0001-1", "10000.00");
        var value = new BigDecimal(amount);
        return switch (type) {
            case "INTERNAL" -> new InternalTransfer(source, Scenario.individualAccount("0002-2", "0.00"), value);
            case "BETWEEN_CREDIT_UNIONS" -> new InterCreditUnionTransfer(source, new CreditUnionAccount("0102", "7788-1"), value);
            default -> new OtherBankTransfer(source, Scenario.EXTERNAL_ACCOUNT, value);
        };
    }

    @ParameterizedTest(name = "{0} of {1} pays {2}")
    @CsvSource({
            "INTERNAL,              500.00,  0.00",
            "BETWEEN_CREDIT_UNIONS, 500.00,  1.50",
            "TO_OTHER_BANK,         100.00,  0.00",
            "TO_OTHER_BANK,         100.01,  3.00",
            "TO_OTHER_BANK,         500.00,  3.00",
            "TO_OTHER_BANK,        2000.00, 10.00"
    })
    @DisplayName("switch with patterns over the sealed hierarchy")
    void calculatesByType(String type, BigDecimal amount, BigDecimal expected) {
        assertEquals(expected, calculator.calculate(transfer(type, amount.toPlainString())), "wrong fee");
    }

    @Test
    @DisplayName("rural producer to a cooperative bank is exempt (nested record pattern + guard)")
    void ruralProducerToCooperativeBankIsExempt() {
        var producer = Scenario.account("0005-5", new Member("39053344705", "Boa Vista Farm", MemberType.RURAL_PRODUCER),
                "10000.00");
        var toCooperative = new OtherBankTransfer(producer, new ExternalAccount("02038232", "0001", "123-4"),
                new BigDecimal("2000.00"));
        var toOther = new OtherBankTransfer(producer, Scenario.EXTERNAL_ACCOUNT, new BigDecimal("2000.00"));
        assertEquals(new BigDecimal("0.00"), calculator.calculate(toCooperative));
        assertEquals(new BigDecimal("10.00"), calculator.calculate(toOther));
    }
}
