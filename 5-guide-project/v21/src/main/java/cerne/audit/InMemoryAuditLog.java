package cerne.audit;

import java.util.ArrayList;
import java.util.List;

import cerne.transfers.TransferReceipt;
import cerne.transfers.TransferRequest;

/** Log for tests: keeps only the name of each event, in order. */
public class InMemoryAuditLog implements AuditLog {

    private final List<String> events = new ArrayList<>();

    @Override
    public synchronized void recordAttempt(TransferRequest request) {
        events.add("ATTEMPT");
    }

    @Override
    public synchronized void recordSuccess(TransferReceipt receipt) {
        events.add("SUCCESS");
    }

    @Override
    public synchronized void recordFailure(TransferRequest request, RuntimeException error) {
        events.add("FAILURE");
    }

    public synchronized List<String> events() {
        return List.copyOf(events);
    }
}
