package cerne.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cerne.transfers.TransferReceipt;
import cerne.transfers.TransferRequest;

/**
 * Audit trail in JSON with Jackson (session 8). Replaces XStream 1.4.9: no deep reflection into the JDK,
 * no --add-opens. The old XML lines remain readable by the archiving system.
 */
@ApplicationScoped
public class AuditService implements AuditLog {

    private final ObjectMapper json = new ObjectMapper();
    private final List<String> records = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void recordAttempt(TransferRequest request) {
        write(new AuditEvent("ATTEMPT", request.idempotencyKey().value(),
                request.sourceAccount(), request.amount(), ""));
    }

    @Override
    public void recordSuccess(TransferReceipt receipt) {
        var request = receipt.request();
        write(new AuditEvent("SUCCESS", request.idempotencyKey().value(),
                request.sourceAccount(), request.amount(), receipt.identifier()));
    }

    @Override
    public void recordFailure(TransferRequest request, RuntimeException error) {
        write(new AuditEvent("FAILURE", request.idempotencyKey().value(),
                request.sourceAccount(), request.amount(), error.getMessage()));
    }

    public List<String> records() {
        return List.copyOf(records);
    }

    private void write(AuditEvent event) {
        try {
            records.add(json.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize audit event", e);
        }
    }
}
