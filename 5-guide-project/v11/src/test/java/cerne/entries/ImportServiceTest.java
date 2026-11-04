package cerne.entries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

import cerne.entries.imports.FileLine;
import cerne.entries.imports.ImportService;

public class ImportServiceTest {

    private final ImportService service = new ImportService();

    @Test
    public void creditLineBecomesCreditEntryWithSource() {
        FileLine line = new FileLine("C", "0001-1", BigDecimal.TEN, LocalDate.of(2026, 8, 5));
        line.setSource("TELLER_DEPOSIT");
        Entry entry = service.buildEntry(line);
        assertTrue(entry instanceof CreditEntry);
        assertEquals("TELLER_DEPOSIT", ((CreditEntry) entry).getSource());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownTypeIsRejected() {
        service.buildEntry(new FileLine("X", "0001-1", BigDecimal.TEN, LocalDate.of(2026, 8, 5)));
    }
}
