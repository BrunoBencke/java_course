package cerne.statements;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import cerne.entries.Entry;
import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.FeeEntry;

public class StatementGeneratorTest {

    private final StatementGenerator generator = new StatementGenerator();

    private List<Entry> threeEntries() {
        var credit = new CreditEntry("0001-1", new BigDecimal("150.00"), LocalDate.of(2026, 8, 5));
        credit.setSource("TELLER_DEPOSIT");
        var fee = new FeeEntry("0001-1", new BigDecimal("3.00"), LocalDate.of(2026, 8, 7));
        fee.setFeeCode("TED");
        return List.of(credit, new DebitEntry("0001-1", new BigDecimal("40.00"), LocalDate.of(2026, 8, 6)), fee);
    }

    @Test
    public void csvHasOneLinePerEntryWithSign() {
        assertEquals("05/08/2026;CREDIT;150.00;TELLER_DEPOSIT\n"
                + "06/08/2026;DEBIT;-40.00;\n"
                + "07/08/2026;FEE;-3.00;TED\n", generator.generateCsv(threeEntries()));
    }

    @Test
    public void textUsesAmountInReais() {
        assertEquals("05/08/2026  CREDIT   +R$ 150,00", generator.generateText(threeEntries()).split("\n")[0]);
    }
}
