package cerne.externallookup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cerne.members.Cpf;

/** Local simulator of the CPF service: never touches the network. Used in the tests and measurements. */
public class FakeCpfLookup implements CpfLookup {

    private final long latencyInMillis;
    private final Map<Cpf, CpfStatus> statuses = new ConcurrentHashMap<>();

    public FakeCpfLookup() {
        this(0);
    }

    /** The latency simulates the network wait with Thread.sleep (which releases the thread like real I/O). */
    public FakeCpfLookup(long latencyInMillis) {
        this.latencyInMillis = latencyInMillis;
    }

    public FakeCpfLookup with(Cpf cpf, CpfStatus status) {
        statuses.put(cpf, status);
        return this;
    }

    @Override
    public CpfStatus lookUp(Cpf cpf) {
        if (latencyInMillis > 0) {
            try {
                Thread.sleep(latencyInMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Lookup interrupted", e);
            }
        }
        return statuses.getOrDefault(cpf, CpfStatus.REGULAR);
    }
}
