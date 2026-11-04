package cerne.statements;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import cerne.entries.Entry;

public class Statement {

    private final String accountNumber;
    private final Date start;
    private final Date end;
    private final List<Entry> entries;

    public Statement(String accountNumber, Date start, Date end, List<Entry> entries) {
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

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public List<Entry> getEntries() {
        return entries;
    }
}
