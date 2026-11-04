package cerne.statements;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.Entry;
import cerne.entries.FeeEntry;
import cerne.util.CerneUtil;

public class StatementGeneratorTest {

    private final StatementGenerator generator = new StatementGenerator();

    private List<Entry> threeEntries() {
        CreditEntry credit = new CreditEntry("0001-1", 150.0, CerneUtil.date(2026, 8, 5));
        credit.setSource("TELLER_DEPOSIT");
        FeeEntry fee = new FeeEntry("0001-1", 3.0, CerneUtil.date(2026, 8, 7));
        fee.setFeeCode("TED");
        return Arrays.<Entry>asList(credit, new DebitEntry("0001-1", 40.0, CerneUtil.date(2026, 8, 6)), fee);
    }

    @Test
    public void csvHasOneLinePerEntryWithSign() {
        assertEquals("05/08/2026;CREDIT;150.0;TELLER_DEPOSIT\n"
                + "06/08/2026;DEBIT;-40.0;\n"
                + "07/08/2026;FEE;-3.0;TED\n", generator.generateCsv(threeEntries()));
    }

    @Test
    public void textUsesAmountInReais() {
        assertEquals("05/08/2026  CREDIT   +R$ 150,00", generator.generateText(threeEntries()).split("\n")[0]);
    }
}
