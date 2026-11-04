package cerne.transfers;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compares the transfers recorded in Cerne with the central's daily file (session 2 challenge).
 * Matches by identifier (no spaces, upper case) and amount; each file line matches only once.
 */
public class TransferReconciliation {

    public ReconciliationResult reconcile(List<TransferRecord> fromCerne,
                                          List<TransferRecord> fromFile) {
        // groupingBy and not toMap: the file may repeat an identifier and toMap would throw "Duplicate key"
        Map<Key, Deque<TransferRecord>> index = fromFile.stream()
                .collect(Collectors.groupingBy(Key::of, HashMap::new, Collectors.toCollection(ArrayDeque::new)));

        // sequential stream on purpose: the predicate consumes the index in Cerne's order
        Set<TransferRecord> matchedInFile = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<Boolean, List<TransferRecord>> byMatch = fromCerne.stream()
                .collect(Collectors.partitioningBy(fromSystem -> takeFromIndex(index, fromSystem)
                        .map(matchedInFile::add)
                        .orElse(false)));

        var matched = byMatch.get(true);
        var onlyInFile = fromFile.stream()
                .filter(record -> !matchedInFile.contains(record))
                .collect(Collectors.toList());
        var total = matched.stream()
                .map(TransferRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var countByDate = matched.stream()
                .collect(Collectors.groupingBy(TransferRecord::getDate, Collectors.summingInt(record -> 1)));

        return new ReconciliationResult(matched, byMatch.get(false), onlyInFile, total,
                countByDate);
    }

    /** The index is consumed on purpose: the same file line does not match twice. */
    private Optional<TransferRecord> takeFromIndex(Map<Key, Deque<TransferRecord>> index,
                                                   TransferRecord fromSystem) {
        return Optional.ofNullable(index.get(Key.of(fromSystem))).map(Deque::pollFirst);
    }

    /** Matching key: normalized identifier and amount without trailing zeros (10.0 == 10.00). */
    private static final class Key {

        private final String identifier;
        private final BigDecimal amount;

        private Key(String identifier, BigDecimal amount) {
            this.identifier = identifier;
            this.amount = amount;
        }

        static Key of(TransferRecord record) {
            return new Key(record.getIdentifier().strip().toUpperCase(),
                    record.getAmount().stripTrailingZeros());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) {
                return false;
            }
            Key other = (Key) o;
            return identifier.equals(other.identifier) && amount.equals(other.amount);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifier, amount);
        }
    }
}
