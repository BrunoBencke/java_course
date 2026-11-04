package cerne.entries;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryEntryRepository implements EntryRepository {

    private final Map<String, List<Entry>> byAccount = new HashMap<String, List<Entry>>();

    @Override
    public void save(Entry entry) {
        List<Entry> ofAccount = byAccount.get(entry.getAccountNumber());
        if (ofAccount == null) {
            ofAccount = new ArrayList<Entry>();
            byAccount.put(entry.getAccountNumber(), ofAccount);
        }
        ofAccount.add(entry);
    }

    @Override
    public List<Entry> findByAccount(String accountNumber, Date start, Date end) {
        List<Entry> ofAccount = byAccount.get(accountNumber);
        if (ofAccount == null) {
            return null;
        }
        List<Entry> result = new ArrayList<Entry>();
        for (Entry entry : ofAccount) {
            if (!entry.getDate().before(start) && !entry.getDate().after(end)) {
                result.add(entry);
            }
        }
        return result;
    }
}
