package cerne.measurement;

import java.util.Arrays;
import java.util.function.Supplier;

/** Session 6 harness: same code in all four milestones. Warmup, N runs, median. */
public final class ProcessingMeter {

    private ProcessingMeter() {
    }

    public static long measure(int warmup, int runs, Supplier<ProcessingSummary> processing) {
        for (int i = 0; i < warmup; i++) {
            processing.get(); // result discarded
        }
        long[] times = new long[runs];
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            ProcessingSummary summary = processing.get();
            times[i] = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("run %d: %d ms, %d entries, %d accounts, consolidated balance %s%n",
                    i + 1, times[i], summary.totalEntries(), summary.totalAccounts(),
                    summary.consolidatedBalance().toPlainString());
        }
        Arrays.sort(times);
        long median = times[runs / 2];
        System.out.printf("median: %d ms (min %d, max %d)%n", median, times[0], times[runs - 1]);
        return median;
    }
}
