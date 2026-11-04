package cerne.members;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CpfTest {

    @ParameterizedTest(name = "{0} valid? {1}")
    @CsvSource({
            "529.982.247-25, true",
            "11144477735,    true",
            "12345678909,    true",
            "52998224700,    false",
            "11111111111,    false"
    })
    @DisplayName("checks the check digits")
    void checksDigits(String number, boolean expected) {
        assertEquals(expected, Cpf.isValid(number));
    }

    @Test
    @DisplayName("normalizes punctuation and formats")
    void normalizesAndFormats() {
        var cpf = new Cpf("529.982.247-25");
        assertEquals("52998224725", cpf.number());
        assertEquals("529.982.247-25", cpf.formatted());
    }

    @Test
    @DisplayName("rejects wrong length in the compact constructor")
    void rejectsWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> new Cpf("123"));
    }
}
