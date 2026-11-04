package cerne.externallookup;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cerne.members.Cpf;

/**
 * Prevents two simultaneous lookups of the same CPF with synchronized. On a virtual thread, waiting for I/O inside
 * synchronized pins the carrier (pinning): only "number of cores" lookups make progress at a time.
 * See it with -Djdk.tracePinnedThreads=full. The fix is in ReentrantLockCpfLookup.
 */
public class PerCpfLockingCpfLookup implements CpfLookup {

    private final CpfLookup delegate;
    private final ConcurrentMap<Cpf, Object> locks = new ConcurrentHashMap<>();

    public PerCpfLockingCpfLookup(CpfLookup delegate) {
        this.delegate = delegate;
    }

    @Override
    public CpfStatus lookUp(Cpf cpf) {
        synchronized (locks.computeIfAbsent(cpf, key -> new Object())) {
            return delegate.lookUp(cpf);
        }
    }
}
