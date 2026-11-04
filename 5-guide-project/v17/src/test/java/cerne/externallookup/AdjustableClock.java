package cerne.externallookup;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Test Clock that only moves when told to: proves expiration without Thread.sleep. */
public class AdjustableClock extends Clock {

    private Instant now;

    public AdjustableClock(Instant start) {
        this.now = start;
    }

    public void advance(Duration interval) {
        now = now.plus(interval);
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
