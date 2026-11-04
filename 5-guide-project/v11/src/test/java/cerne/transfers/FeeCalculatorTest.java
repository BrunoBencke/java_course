package cerne.transfers;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

import cerne.accounts.Account;

public class FeeCalculatorTest {

    private final FeeCalculator calculator = new FeeCalculator();
    private final Account source = Scenario.individualAccount("0001-1", "1000.00");

    @Test
    public void internalTransferIsExempt() {
        Transfer t = Transfer.internal(source, Scenario.individualAccount("0002-2", "0.00"), new BigDecimal("500.00"));
        assertEquals(new BigDecimal("0.00"), calculator.calculate(t));
    }

    @Test
    public void betweenCreditUnionsHasFixedFee() {
        Transfer t = Transfer.betweenCreditUnions(source, "0102", "7788-1", new BigDecimal("500.00"));
        assertEquals(new BigDecimal("1.50"), calculator.calculate(t));
    }

    @Test
    public void otherBankUpToExemptionLimitPaysNothing() {
        Transfer t = Transfer.toOtherBank(source, "00000208", "1234", "99887-6", new BigDecimal("100.00"));
        assertEquals(new BigDecimal("0.00"), calculator.calculate(t));
    }

    @Test
    public void otherBankAboveLimitPaysAtLeastMinimumFee() {
        Transfer t = Transfer.toOtherBank(source, "00000208", "1234", "99887-6", new BigDecimal("500.00"));
        assertEquals(new BigDecimal("3.00"), calculator.calculate(t));
    }

    @Test
    public void otherBankWithHighAmountPaysPercentage() {
        Transfer t = Transfer.toOtherBank(source, "00000208", "1234", "99887-6", new BigDecimal("2000.00"));
        assertEquals(new BigDecimal("10.00"), calculator.calculate(t));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownTypeThrowsException() {
        Transfer t = Transfer.internal(source, source, BigDecimal.TEN);
        t.setType("PIX");
        calculator.calculate(t);
    }
}
