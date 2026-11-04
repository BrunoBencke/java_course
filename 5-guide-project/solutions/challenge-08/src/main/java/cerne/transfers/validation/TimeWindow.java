package cerne.transfers.validation;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Interval [start, end) in which a transfer type is accepted. */
public record TimeWindow(LocalTime start, LocalTime end, boolean businessDaysOnly) {

    public static final TimeWindow ALL_DAY = new TimeWindow(LocalTime.MIN, LocalTime.MAX, false);
    public static final TimeWindow BETWEEN_CREDIT_UNIONS =
            new TimeWindow(LocalTime.of(6, 0), LocalTime.of(22, 0), false);
    public static final TimeWindow OTHER_BANK =
            new TimeWindow(LocalTime.of(6, 30), LocalTime.of(17, 0), true);

    public boolean allows(LocalDateTime instant) {
        if (businessDaysOnly && isWeekend(instant.getDayOfWeek())) {
            return false;
        }
        var time = instant.toLocalTime();
        return !time.isBefore(start) && (end.equals(LocalTime.MAX) || time.isBefore(end));
    }

    public String description() {
        return start + "–" + end + (businessDaysOnly ? " on business days" : "");
    }

    private static boolean isWeekend(DayOfWeek day) {
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
