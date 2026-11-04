package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Query projection: 72 lines for four fields (becomes a record in session 4). */
public final class EntrySummary {

    private final long id;
    private final LocalDate date;
    private final BigDecimal amount;
    private final EntryType type;

    public EntrySummary(long id, LocalDate date, BigDecimal amount, EntryType type) {
        this.id = id;
        this.date = date;
        this.amount = amount;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public EntryType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EntrySummary other = (EntrySummary) o;
        // the type field was left out when someone added it
        return id == other.id && Objects.equals(date, other.date) && Objects.equals(amount, other.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, amount);
    }

    @Override
    public String toString() {
        return "EntrySummary{id=" + id + ", date=" + date + ", amount=" + amount + ", type=" + type + "}";
    }
}
