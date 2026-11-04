package cerne.statements;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cerne.entries.Credit;
import cerne.entries.Debit;
import cerne.entries.Entry;
import cerne.entries.Fee;
import cerne.entries.Reversal;

class StatementGeneratorTest {

    private final StatementGenerator generator = new StatementGenerator();

    private static List<Entry> fourEntries() {
        return List.of(
                new Credit("0001-1", new BigDecimal("150.00"), LocalDate.of(2026, 8, 5), "TELLER_DEPOSIT"),
                new Debit("0001-1", new BigDecimal("40.00"), LocalDate.of(2026, 8, 6)),
                new Fee("0001-1", new BigDecimal("3.00"), LocalDate.of(2026, 8, 7), "TED"),
                new Reversal("0001-1", new BigDecimal("40.00"), LocalDate.of(2026, 8, 8),
                        UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }

    @Test
    @DisplayName("CSV: one line per entry, debits with a negative sign")
    void csvWithSign() {
        assertEquals("""
                05/08/2026;CREDIT;150.00;TELLER_DEPOSIT
                06/08/2026;DEBIT;-40.00;
                07/08/2026;FEE;-3.00;TED
                08/08/2026;REVERSAL;40.00;00000000-0000-0000-0000-000000000001
                """, generator.generateCsv(fourEntries()));
    }

    @Test
    @DisplayName("text uses the amount in reais")
    void textInReais() {
        assertEquals("05/08/2026  CREDIT   +R$ 150,00", generator.generateText(fourEntries()).lines().findFirst().orElseThrow());
    }
}
