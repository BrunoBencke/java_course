package cerne.transfers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** The three lists of the daily reconciliation, plus two totals. */
public class ReconciliationResult {

    private final List<TransferRecord> matched;
    private final List<TransferRecord> onlyInCerne;
    private final List<TransferRecord> onlyInFile;
    private final BigDecimal totalMatched;
    private final Map<LocalDate, Integer> countByDate;

    public ReconciliationResult(List<TransferRecord> matched,
                                List<TransferRecord> onlyInCerne,
                                List<TransferRecord> onlyInFile,
                                BigDecimal totalMatched,
                                Map<LocalDate, Integer> countByDate) {
        this.matched = matched;
        this.onlyInCerne = onlyInCerne;
        this.onlyInFile = onlyInFile;
        this.totalMatched = totalMatched;
        this.countByDate = countByDate;
    }

    public List<TransferRecord> getMatched() {
        return matched;
    }

    public List<TransferRecord> getOnlyInCerne() {
        return onlyInCerne;
    }

    public List<TransferRecord> getOnlyInFile() {
        return onlyInFile;
    }

    public BigDecimal getTotalMatched() {
        return totalMatched;
    }

    public Map<LocalDate, Integer> getCountByDate() {
        return countByDate;
    }

    /** Identifiers of a list, in order; makes comparing versions easier. */
    public static String identifiers(List<TransferRecord> records) {
        StringBuilder text = new StringBuilder();
        for (TransferRecord record : records) {
            if (text.length() > 0) {
                text.append(",");
            }
            text.append(record.getIdentifier());
        }
        return text.toString();
    }
}
