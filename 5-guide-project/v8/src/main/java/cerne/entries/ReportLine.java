package cerne.entries;

/** One line of the report per member. */
public class ReportLine {

    private final String memberName;
    private final int count;
    private final double totalCredit;
    private final double totalDebit;

    public ReportLine(String memberName, int count, double totalCredit, double totalDebit) {
        this.memberName = memberName;
        this.count = count;
        this.totalCredit = totalCredit;
        this.totalDebit = totalDebit;
    }

    public String getMemberName() {
        return memberName;
    }

    public int getCount() {
        return count;
    }

    public double getTotalCredit() {
        return totalCredit;
    }

    public double getTotalDebit() {
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
                && Double.compare(totalCredit, other.totalCredit) == 0
                && Double.compare(totalDebit, other.totalDebit) == 0
                && memberName.equals(other.memberName);
    }

    @Override
    public int hashCode() {
        return memberName.hashCode() * 31 + count;
    }

    @Override
    public String toString() {
        return memberName + " (" + count + "): +" + totalCredit + " -" + totalDebit;
    }
}
