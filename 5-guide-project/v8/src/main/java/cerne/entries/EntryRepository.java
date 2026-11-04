package cerne.entries;

import java.util.Date;
import java.util.List;

public interface EntryRepository {

    void save(Entry entry);

    /** Returns null when the account never had an entry. */
    List<Entry> findByAccount(String accountNumber, Date start, Date end);
}
