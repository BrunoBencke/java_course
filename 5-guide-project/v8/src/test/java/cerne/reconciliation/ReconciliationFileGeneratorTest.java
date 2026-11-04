package cerne.reconciliation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.Entry;
import cerne.entries.FeeEntry;
import cerne.util.CerneUtil;

/** Real JAXB, in memory. */
public class ReconciliationFileGeneratorTest {

    private final ReconciliationFileGenerator generator = new ReconciliationFileGenerator();

    private List<Entry> threeEntries() {
        return Arrays.<Entry>asList(
                new CreditEntry("0001-1", 150.0, CerneUtil.date(2026, 8, 5)),
                new DebitEntry("0001-1", 40.0, CerneUtil.date(2026, 8, 6)),
                new FeeEntry("0001-1", 3.0, CerneUtil.date(2026, 8, 7)));
    }

    @Test
    public void generatesXmlWithOneElementPerEntry() {
        String xml = generator.generate("0001-1", threeEntries());
        assertTrue(xml.contains("<reconciliation account=\"0001-1\">"));
        assertTrue(xml.contains("<entry date=\"05/08/2026\" type=\"C\" amount=\"150.00\"/>"));
        assertTrue(xml.contains("<total>107.00</total>"));
    }

    @Test
    public void roundTripPreservesContent() {
        ReconciliationFile read = generator.read(generator.generate("0001-1", threeEntries()));
        assertEquals("0001-1", read.getAccount());
        assertEquals(3, read.getItems().size());
        assertEquals("MDAwMS0xfDEwNy4wMHwz", read.getSignature());
    }
}
