package cerne.externallookup;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import cerne.members.Cpf;

/**
 * Batch CPF lookup (nightly reconciliation). The executor comes from outside for the session 5 experiment:
 * a pool of 50 platform threads × one virtual thread per lookup.
 *
 * Usage: BatchLookupService [platform|virtual|pinning|reentrant] [lookups=5000]
 * The simulator waits 200 ms per lookup with Thread.sleep (unmounts like real I/O) and never touches the network.
 */
public class BatchLookupService {

    private final CpfLookup lookup;
    private final Supplier<ExecutorService> executorFactory;

    public BatchLookupService(CpfLookup lookup, Supplier<ExecutorService> executorFactory) {
        this.lookup = lookup;
        this.executorFactory = executorFactory;
    }

    /** Platform pool: the time is lookups ÷ threads × latency, by construction. */
    public static BatchLookupService withPlatformPool(CpfLookup lookup, int threads) {
        return new BatchLookupService(lookup, () -> Executors.newFixedThreadPool(threads));
    }

    /** One virtual thread per task. Do not create a "pool of virtual threads"; to limit, use a Semaphore. */
    public static BatchLookupService withVirtualThreads(CpfLookup lookup) {
        return new BatchLookupService(lookup, Executors::newVirtualThreadPerTaskExecutor);
    }

    public List<CpfStatus> lookUp(List<Cpf> cpfs) {
        try (ExecutorService executor = executorFactory.get()) {  // close() waits for all tasks
            List<Future<CpfStatus>> futures = cpfs.stream()
                    .map(cpf -> executor.submit(() -> lookup.lookUp(cpf)))
                    .toList();
            return futures.stream().map(BatchLookupService::result).toList();
        }
    }

    private static CpfStatus result(Future<CpfStatus> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Batch interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Lookup failed", e.getCause());
        }
    }

    public static void main(String[] args) {
        var variant = args.length > 0 ? args[0] : "virtual";
        var count = args.length > 1 ? Integer.parseInt(args[1]) : 5_000;
        var simulator = new FakeCpfLookup(200);

        record Experiment(String description, BatchLookupService service) {
        }
        var experiment = switch (variant) {
            case "platform" -> new Experiment("platform(50)", withPlatformPool(simulator, 50));
            case "virtual" -> new Experiment("virtual", withVirtualThreads(simulator));
            case "pinning" -> new Experiment("virtual + synchronized",
                    withVirtualThreads(new PerCpfLockingCpfLookup(simulator)));
            case "reentrant" -> new Experiment("virtual + ReentrantLock",
                    withVirtualThreads(new ReentrantLockCpfLookup(simulator)));
            default -> throw new IllegalArgumentException(
                    "Unknown variant: " + variant + " (use platform, virtual, pinning or reentrant)");
        };

        var cpfs = IntStream.rangeClosed(1, count).mapToObj(i -> new Cpf("%011d".formatted(i))).toList();
        var start = System.nanoTime();
        var statuses = experiment.service().lookUp(cpfs);
        var seconds = (System.nanoTime() - start) / 1e9;
        System.out.printf("%d lookups · executor=%s · %.1f s%n", statuses.size(), experiment.description(), seconds);
    }
}
