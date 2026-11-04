package cerne.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Matches Cerne's entries against the records from the other credit union. */
public class EntryReconciler {

    public List<ReconciliationOutcome> reconcile(List<InternalEntry> internals, List<ExternalRecord> externals,
                                                 LocalDate today) {
        // toMap with merge: a record repeated in the external file counts once
        Map<String, ExternalRecord> byIdentifier = externals.stream()
                .collect(Collectors.toMap(ExternalRecord::identifier, Function.identity(), (a, b) -> a));
        return internals.stream()
                .map(entry -> Optional.ofNullable(byIdentifier.get(entry.identifier()))
                        .<ReconciliationOutcome>map(external -> external.amount().compareTo(entry.amount()) == 0
                                ? new Matched(entry, external)
                                : new Mismatched(entry, external))
                        .orElseGet(() -> new Pending(entry,
                                (int) ChronoUnit.DAYS.between(entry.date(), today))))
                .toList();
    }

    public long pendingCount(List<ReconciliationOutcome> outcomes) {
        return outcomes.stream().filter(outcome -> outcome instanceof Pending).count();
    }

    public BigDecimal totalDifference(List<ReconciliationOutcome> outcomes) {
        var total = BigDecimal.ZERO;
        for (var outcome : outcomes) {
            if (outcome instanceof Mismatched m) {
                total = total.add(m.entry().amount().subtract(m.externalRecord().amount()).abs());
            }
        }
        return total;
    }
}
