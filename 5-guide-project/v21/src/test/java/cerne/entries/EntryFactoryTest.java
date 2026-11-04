package cerne.entries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cerne.entries.imports.FileLine;

class EntryFactoryTest {

    @ParameterizedTest(name = "code {0} creates {1}")
    @CsvSource({"C, Credit", "D, Debit", "T, Fee", "E, Reversal"})
    @DisplayName("creates the right concrete type from the file code")
    void createsTheRightConcreteType(String code, String expectedClass) {
        var line = FileLine.forTest(code);
        assertEquals(expectedClass, EntryFactory.from(line).getClass().getSimpleName());
    }

    @Test
    @DisplayName("rejects an unknown code")
    void rejectsUnknownCode() {
        assertThrows(UnknownEntryTypeException.class,
                () -> EntryFactory.from(FileLine.forTest("X")));
    }
}
