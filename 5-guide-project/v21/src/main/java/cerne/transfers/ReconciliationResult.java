package cerne.transfers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** The three lists of the daily reconciliation, plus two totals. */
public record ReconciliationResult(List<TransferRecord> matched,
                                   List<TransferRecord> onlyInCerne,
                                   List<TransferRecord> onlyInFile,
                                   BigDecimal totalMatched,
                                   Map<LocalDate, Integer> countByDate) {

    public ReconciliationResult {
        matched = List.copyOf(matched);
        onlyInCerne = List.copyOf(onlyInCerne);
        onlyInFile = List.copyOf(onlyInFile);
        countByDate = Map.copyOf(countByDate);
    }

    /** Identifiers of a list, in order; makes comparing versions easier. */
    public static String identifiers(List<TransferRecord> records) {
        return records.stream().map(TransferRecord::identifier).collect(Collectors.joining(","));
    }
}
