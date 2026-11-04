package cerne.transfers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Validation in the compact constructors (session 4 challenge). */
class TransferTest {

    @ParameterizedTest(name = "amount {0}")
    @CsvSource({"0", "-10.00"})
    @DisplayName("none of the three types accepts a zero or negative amount")
    void rejectsNonPositiveAmount(BigDecimal amount) {
        var source = Scenario.individualAccount("0001-1", "100.00");
        assertThrows(IllegalArgumentException.class,
                () -> new InternalTransfer(source, Scenario.individualAccount("0002-2", "0.00"), amount));
        assertThrows(IllegalArgumentException.class,
                () -> new InterCreditUnionTransfer(source, new CreditUnionAccount("0102", "1"), amount));
        assertThrows(IllegalArgumentException.class,
                () -> new OtherBankTransfer(source, Scenario.EXTERNAL_ACCOUNT, amount));
    }

    @ParameterizedTest(name = "ispb=''{0}'' branch=''{1}'' account=''{2}''")
    @CsvSource({"'', 1234, 99", "00000208, ' ', 99", "00000208, 1234, ''", "208, 1234, 99"})
    @DisplayName("destination identifiers cannot be blank")
    void rejectsBlankDestination(String ispb, String branch, String number) {
        assertThrows(IllegalArgumentException.class, () -> new ExternalAccount(ispb, branch, number));
    }

    @Test
    @DisplayName("request exposes amount, source and destination of any type")
    void requestExposesTheRequestData() {
        var source = Scenario.individualAccount("0001-1", "100.00");
        var request = TransferRequest.of(
                new InterCreditUnionTransfer(source, new CreditUnionAccount("0102", "7788-1"), new BigDecimal("5")));
        assertEquals(new BigDecimal("5.00"), request.amount());
        assertEquals("0001-1", request.sourceAccount());
        assertEquals("7788-1", request.destinationAccount());
        assertInstanceOf(InterCreditUnionTransfer.class, request.transfer());
    }
}
