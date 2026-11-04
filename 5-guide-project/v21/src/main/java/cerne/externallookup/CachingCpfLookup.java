package cerne.externallookup;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cerne.members.Cpf;

/**
 * Cache Decorator (session 7): the lookup is slow and billed. The injected Clock makes it possible to test
 * expiration without waiting an hour.
 */
public class CachingCpfLookup implements CpfLookup {

    private record CacheEntry(CpfStatus status, Instant expiresAt) {
    }

    private final CpfLookup delegate;
    private final Duration validity;
    private final Clock clock;
    private final ConcurrentMap<Cpf, CacheEntry> cache = new ConcurrentHashMap<>();

    public CachingCpfLookup(CpfLookup delegate, Duration validity, Clock clock) {
        this.delegate = delegate;
        this.validity = validity;
        this.clock = clock;
    }

    @Override
    public CpfStatus lookUp(Cpf cpf) {
        var now = clock.instant();
        var entry = cache.compute(cpf, (key, current) ->
                current != null && current.expiresAt().isAfter(now)
                        ? current
                        : new CacheEntry(delegate.lookUp(key), now.plus(validity)));
        return entry.status();
    }
}
