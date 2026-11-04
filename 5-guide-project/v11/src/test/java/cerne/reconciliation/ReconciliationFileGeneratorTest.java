package cerne.reconciliation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import cerne.entries.Entry;
import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.FeeEntry;

/** Real JAXB, in memory. The reference file proves that replacing JAXB did not change the output. */
public class ReconciliationFileGeneratorTest {

    private final ReconciliationFileGenerator generator = new ReconciliationFileGenerator();

    private List<Entry> threeEntries() {
        return List.of(
                new CreditEntry("0001-1", new BigDecimal("150.00"), LocalDate.of(2026, 8, 5)),
                new DebitEntry("0001-1", new BigDecimal("40.00"), LocalDate.of(2026, 8, 6)),
                new FeeEntry("0001-1", new BigDecimal("3.00"), LocalDate.of(2026, 8, 7)));
    }

    @Test
    public void generatesXmlWithOneElementPerEntry() {
        String xml = generator.generate("0001-1", threeEntries());
        assertTrue(xml.contains("<reconciliation account=\"0001-1\">"));
        assertTrue(xml.contains("<entry date=\"05/08/2026\" type=\"CREDIT\" amount=\"150.00\"/>"));
    }

    @Test
    public void roundTripPreservesContent() {
        ReconciliationFile read = generator.read(generator.generate("0001-1", threeEntries()));
        assertEquals("0001-1", read.getAccount());
        assertEquals(3, read.getItems().size());
        assertEquals("MDAwMS0xfDEwNy4wMHwz", read.getSignature());
    }

    @Test
    public void outputMatchesReferenceFile() throws IOException {
        // line breaks normalized; attribute order and indentation must match
        String expected = resource("/reconciliation/reference-file.xml");
        assertEquals(normalize(expected), normalize(generator.generate("0001-1", threeEntries())));
    }

    private static String resource(String path) throws IOException {
        try (InputStream input = ReconciliationFileGeneratorTest.class.getResourceAsStream(path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String normalize(String xml) {
        return xml.replace("\r\n", "\n").strip();
    }
}
