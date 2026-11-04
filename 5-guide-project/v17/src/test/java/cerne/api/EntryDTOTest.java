package cerne.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cerne.entries.EntryType;

class EntryDTOTest {

    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("equals covers every component, including source")
    void equalsCoversSource() {
        var day = LocalDate.of(2026, 8, 5);
        assertNotEquals(new EntryDTO("0001-7", day, BigDecimal.TEN, EntryType.CREDIT, "TELLER"),
                new EntryDTO("0001-7", day, BigDecimal.TEN, EntryType.CREDIT, "PIX"));
    }

    @Test
    @DisplayName("JSON → record → JSON, no annotation: the compact constructor normalizes on deserialization")
    void jsonRoundTrip() throws Exception {
        var body = """
                {
                  "accountId": "0001-7",
                  "date": "2026-08-05",
                  "amount": 150.0,
                  "type": "CREDIT",
                  "source": "  TELLER_DEPOSIT  "
                }
                """;
        var dto = json.readValue(body, EntryDTO.class);
        assertEquals(new EntryDTO("0001-7", LocalDate.of(2026, 8, 5), new BigDecimal("150.00"),
                EntryType.CREDIT, "TELLER_DEPOSIT"), dto);
        assertEquals(dto, json.readValue(json.writeValueAsString(dto), EntryDTO.class));
    }

    @Test
    @DisplayName("the compact constructor's validation protects the API boundary")
    void negativeAmountIsRejected() {
        var body = """
                {"accountId": "0001-7", "date": "2026-08-05", "amount": -1, "type": "CREDIT"}
                """;
        assertThrows(ValueInstantiationException.class, () -> json.readValue(body, EntryDTO.class));
    }
}
