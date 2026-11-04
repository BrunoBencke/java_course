package cerne.entries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EntryTest {

    @Test
    @DisplayName("compact constructor normalizes the amount to two decimal places: 0.1 and 0.10 are equal")
    void normalizesScale() {
        var day = LocalDate.of(2026, 8, 5);
        assertEquals(new Debit("0001-1", new BigDecimal("0.10"), day), new Debit("0001-1", new BigDecimal("0.1"), day));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1.00"})
    @DisplayName("rejects a zero or negative amount")
    void rejectsNonPositiveAmount(String amount) {
        assertThrows(IllegalArgumentException.class,
                () -> new Credit("0001-1", new BigDecimal(amount), LocalDate.of(2026, 8, 5), "x"));
    }

    @Test
    @DisplayName("summary is a record: the type is part of equals")
    void summaryComparesTheType() {
        var day = LocalDate.of(2026, 8, 5);
        var credit = new EntrySummary(1, day, BigDecimal.TEN, EntryType.CREDIT);
        var debit = new EntrySummary(1, day, BigDecimal.TEN, EntryType.DEBIT);
        assertEquals(false, credit.equals(debit));
    }
}
