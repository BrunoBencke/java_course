package cerne.externallookup;

import java.util.HashMap;
import java.util.Map;

/** Local simulator of the CPF service: never touches the network. Used in the tests and measurements. */
public class FakeCpfLookup implements CpfLookup {

    private final long latencyInMillis;
    private final Map<String, CpfStatus> statuses = new HashMap<String, CpfStatus>();

    public FakeCpfLookup() {
        this(0);
    }

    /** The latency simulates the network wait with Thread.sleep. */
    public FakeCpfLookup(long latencyInMillis) {
        this.latencyInMillis = latencyInMillis;
    }

    public FakeCpfLookup with(String cpf, CpfStatus status) {
        statuses.put(cpf, status);
        return this;
    }

    @Override
    public CpfStatus lookUp(String cpf) {
        if (latencyInMillis > 0) {
            try {
                Thread.sleep(latencyInMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Lookup interrupted", e);
            }
        }
        CpfStatus status = statuses.get(cpf);
        return status == null ? CpfStatus.REGULAR : status;
    }
}
