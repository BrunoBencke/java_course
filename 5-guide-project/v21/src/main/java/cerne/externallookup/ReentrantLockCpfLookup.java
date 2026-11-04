package cerne.externallookup;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import cerne.members.Cpf;

/**
 * Same per-CPF lock, with ReentrantLock: the virtual thread unmounts while waiting and the carrier stays free.
 * Even better would be to move the network call out of the lock.
 */
public class ReentrantLockCpfLookup implements CpfLookup {

    private final CpfLookup delegate;
    private final ConcurrentMap<Cpf, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLockCpfLookup(CpfLookup delegate) {
        this.delegate = delegate;
    }

    @Override
    public CpfStatus lookUp(Cpf cpf) {
        var lock = locks.computeIfAbsent(cpf, key -> new ReentrantLock());
        lock.lock();
        try {
            return delegate.lookUp(cpf);
        } finally {
            lock.unlock();
        }
    }
}
