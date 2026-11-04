package cerne.measurement;

import java.util.Arrays;
import java.util.function.Supplier;

/** Minimal stopwatch: warmup, N repetitions, median. For a real decision, use JMH. */
public final class Stopwatch {

    private Stopwatch() {
    }

    public static <T> T measure(String label, int warmup, int repetitions, Supplier<T> action) {
        for (int i = 0; i < warmup; i++) {
            action.get();
        }
        long[] durations = new long[repetitions];
        T result = null;
        for (int i = 0; i < repetitions; i++) {
            long start = System.nanoTime();
            result = action.get();
            durations[i] = (System.nanoTime() - start) / 1_000_000;
        }
        Arrays.sort(durations);
        System.out.printf("%s: median %d ms (min %d, max %d)%n",
                label, durations[repetitions / 2], durations[0], durations[repetitions - 1]);
        return result;
    }
}
