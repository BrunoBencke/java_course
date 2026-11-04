package cerne.measurement;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Session 6 entry processing: reads N synthetic lines (fixed seed), calculates balance and fee per
 * account and builds the consolidated statement. The code is identical in all four milestones (Java 8
 * syntax) so that the only variable in the measurement is the JDK.
 *
 * Usage: EntryProcessing [warmups=3] [runs=5] [entries=1000000]
 */
public final class EntryProcessing {

    private static final int ACCOUNT_COUNT = 100_000;
    private static final BigDecimal FEE_PER_FEE_ENTRY = new BigDecimal("0.35");
    private static final BigDecimal MONTHLY_PACKAGE = new BigDecimal("12.90");

    private EntryProcessing() {
    }

    public static void main(String[] args) {
        int warmups = args.length > 0 ? Integer.parseInt(args[0]) : 3;
        int runs = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        int count = args.length > 2 ? Integer.parseInt(args[2]) : 1_000_000;

        System.out.printf("JDK %s · collectors: %s · max heap: %d MB%n", System.getProperty("java.version"),
                collectors(), Runtime.getRuntime().maxMemory() / (1024 * 1024));
        final List<String> lines = DatasetGenerator.entryLines(count, ACCOUNT_COUNT);
        System.out.printf("dataset: %d synthetic entries · warmup %d · runs %d%n",
                lines.size(), warmups, runs);
        ProcessingMeter.measure(warmups, runs, new Supplier<ProcessingSummary>() {
            @Override
            public ProcessingSummary get() {
                return process(lines);
            }
        });
    }

    public static ProcessingSummary process(List<String> lines) {
        Map<String, AccountTotals> byAccount = new HashMap<String, AccountTotals>();
        for (String line : lines) {
            String[] fields = line.split(";");
            AccountTotals totals = byAccount.get(fields[0]);
            if (totals == null) {
                totals = new AccountTotals();
                byAccount.put(fields[0], totals);
            }
            totals.record(fields[2].charAt(0), new BigDecimal(fields[3]), LocalDate.parse(fields[1]));
        }

        List<String> consolidatedStatement = new ArrayList<String>(byAccount.size());
        BigDecimal consolidatedBalance = BigDecimal.ZERO;
        for (Map.Entry<String, AccountTotals> mapEntry : new TreeMap<String, AccountTotals>(byAccount).entrySet()) {
            AccountTotals totals = mapEntry.getValue();
            BigDecimal fee = totals.monthlyFee();
            BigDecimal balance = totals.balance().subtract(fee);
            consolidatedStatement.add(mapEntry.getKey() + ";" + totals.count + ";" + balance.toPlainString()
                    + ";" + fee.toPlainString() + ";" + totals.lastDay);
            consolidatedBalance = consolidatedBalance.add(balance);
        }
        return new ProcessingSummary(lines.size(), consolidatedStatement.size(), consolidatedBalance);
    }

    private static String collectors() {
        StringBuilder names = new StringBuilder();
        for (GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(collector.getName());
        }
        return names.toString();
    }

    /** Mutable accumulator per account. */
    private static final class AccountTotals {

        private BigDecimal credits = BigDecimal.ZERO;
        private BigDecimal debits = BigDecimal.ZERO;
        private int feeEntries;
        private int count;
        private LocalDate lastDay = LocalDate.MIN;

        void record(char type, BigDecimal amount, LocalDate date) {
            count++;
            if (type == 'C') {
                credits = credits.add(amount);
            } else {
                debits = debits.add(amount);
                if (type == 'T') {
                    feeEntries++;
                }
            }
            if (date.isAfter(lastDay)) {
                lastDay = date;
            }
        }

        BigDecimal balance() {
            return credits.subtract(debits);
        }

        BigDecimal monthlyFee() {
            BigDecimal fee = FEE_PER_FEE_ENTRY.multiply(BigDecimal.valueOf(feeEntries));
            return count > 15 ? fee.add(MONTHLY_PACKAGE) : fee;
        }
    }
}
