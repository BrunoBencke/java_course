package cerne.measurement;

import java.math.BigDecimal;

/** What the processing produced; the totals prove that the versions did the same work. */
public final class ProcessingSummary {

    private final int totalEntries;
    private final int totalAccounts;
    private final BigDecimal consolidatedBalance;

    public ProcessingSummary(int totalEntries, int totalAccounts, BigDecimal consolidatedBalance) {
        this.totalEntries = totalEntries;
        this.totalAccounts = totalAccounts;
        this.consolidatedBalance = consolidatedBalance;
    }

    public int totalEntries() {
        return totalEntries;
    }

    public int totalAccounts() {
        return totalAccounts;
    }

    public BigDecimal consolidatedBalance() {
        return consolidatedBalance;
    }
}
