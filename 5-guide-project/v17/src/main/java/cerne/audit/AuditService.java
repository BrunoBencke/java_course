package cerne.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import com.thoughtworks.xstream.XStream;

import cerne.transfers.TransferReceipt;
import cerne.transfers.TransferRequest;

/**
 * XML audit trail via XStream 1.4.9. On 17 it only works with the --add-opens from the pom
 * (InaccessibleObjectException without them). Replaced in session 8.
 */
@ApplicationScoped
public class AuditService implements AuditLog {

    private final XStream xstream;
    private final List<String> records = Collections.synchronizedList(new ArrayList<>());

    public AuditService() {
        xstream = new XStream();
        xstream.alias("event", AuditEvent.class);
    }

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
        records.add(xstream.toXML(event));
    }
}
