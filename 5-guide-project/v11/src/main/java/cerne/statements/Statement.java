package cerne.statements;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import cerne.entries.Entry;

public class Statement {

    private final String accountNumber;
    private final LocalDate start;
    private final LocalDate end;
    private final List<Entry> entries;

    public Statement(String accountNumber, LocalDate start, LocalDate end, List<Entry> entries) {
        this.accountNumber = accountNumber;
        this.start = start;
        this.end = end;
        this.entries = entries;
    }

    public static Statement empty(String accountNumber) {
        return new Statement(accountNumber, null, null, Collections.<Entry>emptyList());
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public LocalDate getStart() {
        return start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public List<Entry> getEntries() {
        return entries;
    }
}
