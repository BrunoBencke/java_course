package cerne.externallookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cerne.members.Cpf;

class CachingCpfLookupTest {

    @Test
    @DisplayName("repeated lookup within the validity period does not call the delegate")
    void repeatedLookupWithinValidityDoesNotCallTheDelegate() {
        var calls = new AtomicInteger();
        CpfLookup slow = cpf -> {
            calls.incrementAndGet();
            return CpfStatus.REGULAR;
        };
        var clock = Clock.fixed(Instant.parse("2026-09-25T10:00:00Z"), ZoneOffset.UTC);
        var lookup = new CachingCpfLookup(slow, Duration.ofHours(1), clock);

        lookup.lookUp(new Cpf("12345678909"));
        lookup.lookUp(new Cpf("12345678909"));

        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("looks up again after the cache expires")
    void looksUpAgainAfterTheCacheExpires() {
        var calls = new AtomicInteger();
        CpfLookup slow = cpf -> {
            calls.incrementAndGet();
            return CpfStatus.REGULAR;
        };
        var start = Instant.parse("2026-09-25T10:00:00Z");
        var clock = new AdjustableClock(start);
        var lookup = new CachingCpfLookup(slow, Duration.ofHours(1), clock);

        lookup.lookUp(new Cpf("12345678909"));
        clock.advance(Duration.ofMinutes(61));
        lookup.lookUp(new Cpf("12345678909"));

        assertEquals(2, calls.get());
    }
}
