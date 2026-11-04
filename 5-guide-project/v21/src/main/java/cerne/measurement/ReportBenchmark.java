package cerne.measurement;

import java.time.LocalDate;
import java.util.List;

import cerne.entries.EntryReport;
import cerne.entries.LegacyEntryReport;
import cerne.entries.ReportLine;

/**
 * Session 2: measures nested loops × Streams with the same dataset and proves the result is identical.
 * Usage: ReportBenchmark [members=5000] [entries=50000]. The default fits in class (legacy in the range of
 * seconds). With 20000 200000 the legacy takes about 70 s per round; with 2 million it does not finish.
 */
public final class ReportBenchmark {

    private ReportBenchmark() {
    }

    public static void main(String[] args) {
        int memberCount = args.length > 0 ? Integer.parseInt(args[0]) : 5_000;
        int entryCount = args.length > 1 ? Integer.parseInt(args[1]) : 50_000;

        var members = DatasetGenerator.members(memberCount);
        var entries = DatasetGenerator.entries(members, entryCount);
        var start = LocalDate.of(2026, 8, 1);
        var end = LocalDate.of(2026, 8, 31);
        System.out.printf("dataset: %d members, %d entries%n", members.size(), entries.size());

        var legacy = new LegacyEntryReport();
        var current = new EntryReport();

        List<ReportLine> a = Stopwatch.measure("nested loops", 2, 5,
                () -> legacy.generate(members, entries, start, end));
        List<ReportLine> b = Stopwatch.measure("streams", 2, 5,
                () -> current.generate(members, entries, start, end));

        if (!a.equals(b)) {
            throw new AssertionError("The two versions differ; the measurement is worthless");
        }
        System.out.printf("identical result: %d lines%n", a.size());
    }
}
