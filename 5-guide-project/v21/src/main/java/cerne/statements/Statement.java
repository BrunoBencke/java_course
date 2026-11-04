package cerne.statements;

import java.time.LocalDate;
import java.util.List;

import cerne.entries.Entry;

/** Statement of an account: period covered and entries from the most recent to the oldest. */
public record Statement(String accountNumber, LocalDate start, LocalDate end, List<Entry> entries) {

    public static Statement empty(String accountNumber) {
        return new Statement(accountNumber, null, null, List.of());
    }
}
