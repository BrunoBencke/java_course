package cerne.transfers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FeeCalculatorTest {

    private final FeeCalculator calculator = new FeeCalculator();

    private static Transfer transfer(TransferType type, String amount) {
        var source = Scenario.individualAccount("0001-1", "10000.00");
        var value = new BigDecimal(amount);
        return switch (type) {
            case INTERNAL -> new InternalTransfer(source, Scenario.individualAccount("0002-2", "0.00"), value);
            case BETWEEN_CREDIT_UNIONS -> new InterCreditUnionTransfer(source, new CreditUnionAccount("0102", "7788-1"), value);
            case TO_OTHER_BANK -> new OtherBankTransfer(source, Scenario.EXTERNAL_ACCOUNT, value);
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
    @DisplayName("switch expression over the discriminator")
    void calculatesBySwitchExpression(TransferType type, BigDecimal amount, BigDecimal expected) {
        assertEquals(expected, calculator.calculate(transfer(type, amount.toPlainString())), "wrong fee");
    }

    @ParameterizedTest(name = "{0} of {1} pays {2}")
    @CsvSource({
            "INTERNAL,              500.00,  0.00",
            "BETWEEN_CREDIT_UNIONS, 500.00,  1.50",
            "TO_OTHER_BANK,         100.00,  0.00",
            "TO_OTHER_BANK,        2000.00, 10.00"
    })
    @DisplayName("instanceof ladder with pattern matching gives the same result")
    void calculatesByInstanceof(TransferType type, BigDecimal amount, BigDecimal expected) {
        assertEquals(expected, calculator.calculateFee(transfer(type, amount.toPlainString())));
    }
}
