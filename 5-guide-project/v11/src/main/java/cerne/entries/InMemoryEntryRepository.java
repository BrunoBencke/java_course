package cerne.entries;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryEntryRepository implements EntryRepository {

    private final Map<String, List<Entry>> byAccount = new HashMap<>();

    @Override
    public void save(Entry entry) {
        byAccount.computeIfAbsent(entry.getAccountNumber(), number -> new ArrayList<>()).add(entry);
    }

    @Override
    public List<Entry> findByAccount(String accountNumber, LocalDate start, LocalDate end) {
        return byAccount.getOrDefault(accountNumber, List.of()).stream()
                .filter(entry -> entry.isBetween(start, end))
                .collect(Collectors.toList());
    }
}
