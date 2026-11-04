package cerne.entries;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** One report line per member. Totals normalized to 2 decimal places: 150.0 and 150.00 are equal. */
public class ReportLine {

    private final String memberName;
    private final int count;
    private final BigDecimal totalCredit;
    private final BigDecimal totalDebit;

    public ReportLine(String memberName, int count, BigDecimal totalCredit, BigDecimal totalDebit) {
        this.memberName = memberName;
        this.count = count;
        this.totalCredit = totalCredit.setScale(2, RoundingMode.HALF_EVEN);
        this.totalDebit = totalDebit.setScale(2, RoundingMode.HALF_EVEN);
    }

    public String getMemberName() {
        return memberName;
    }

    public int getCount() {
        return count;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportLine)) {
            return false;
        }
        ReportLine other = (ReportLine) o;
        return count == other.count
                && memberName.equals(other.memberName)
                && totalCredit.equals(other.totalCredit)
                && totalDebit.equals(other.totalDebit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberName, count, totalCredit, totalDebit);
    }

    @Override
    public String toString() {
        return memberName + " (" + count + "): +" + totalCredit + " -" + totalDebit;
    }
}
