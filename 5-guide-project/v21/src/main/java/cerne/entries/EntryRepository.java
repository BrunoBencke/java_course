package cerne.entries;

import java.time.LocalDate;
import java.util.List;

public interface EntryRepository {

    void save(Entry entry);

    /** Empty list when the account has no entry in the period. */
    List<Entry> findByAccount(String accountNumber, LocalDate start, LocalDate end);
}
