package cerne.api;

import java.util.Date;

import cerne.entries.CreditEntry;
import cerne.entries.Entry;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** Entry as it leaves the API (JSON). */
@Getter
@Setter
@EqualsAndHashCode
public class EntryDTO {

    private String accountId;
    private Date date;
    private double amount;
    private String type;
    private String source;

    public EntryDTO() {
    }

    public EntryDTO(String accountId, Date date, double amount, String type, String source) {
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
