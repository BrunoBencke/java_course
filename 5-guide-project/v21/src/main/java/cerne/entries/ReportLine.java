package cerne.entries;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** One report line per member; the record's equals covers the four components. */
public record ReportLine(String memberName, int count, BigDecimal totalCredit,
                         BigDecimal totalDebit) {

    public ReportLine {
        totalCredit = totalCredit.setScale(2, RoundingMode.HALF_EVEN);
        totalDebit = totalDebit.setScale(2, RoundingMode.HALF_EVEN);
    }
}
