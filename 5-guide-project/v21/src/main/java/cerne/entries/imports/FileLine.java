package cerne.entries.imports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** One line of the central's entries file; the type comes as a code ("C", "D", "T", "E"). */
public record FileLine(String type, String account, BigDecimal amount, LocalDate date, String source,
                       String feeCode, UUID originalEntry) {

    private static final UUID SAMPLE_ORIGINAL = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /** Complete line with the requested code; used in tests and examples. */
    public static FileLine forTest(String code) {
        return new FileLine(code, "0001-1", new BigDecimal("10.00"), LocalDate.of(2026, 8, 5),
                "TELLER_DEPOSIT", "TED", SAMPLE_ORIGINAL);
    }
}
