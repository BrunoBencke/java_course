package cerne.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import cerne.entries.Entry;
import cerne.entries.CreditEntry;
import cerne.entries.EntryType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** Entry as it leaves the API (JSON). Still mutable and dependent on Lombok. */
@Getter
@Setter
@EqualsAndHashCode
public class EntryDTO {

    private String accountId;
    private LocalDate date;
    private BigDecimal amount;
    private EntryType type;
    private String source;

    public EntryDTO() {
    }

    public EntryDTO(String accountId, LocalDate date, BigDecimal amount, EntryType type, String source) {
        this.accountId = accountId;
        this.date = date;
        this.amount = amount;
        this.type = type;
        this.source = source;
    }

    public static EntryDTO from(Entry entry) {
        String source = entry instanceof CreditEntry
                ? ((CreditEntry) entry).getSource()
                : "";
        return new EntryDTO(entry.getAccountNumber(), entry.getDate(), entry.getAmount(),
                entry.getType(), source);
    }
}
