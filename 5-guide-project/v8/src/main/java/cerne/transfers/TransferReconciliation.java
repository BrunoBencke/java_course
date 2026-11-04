package cerne.transfers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares the transfers recorded in Cerne with the central's daily file.
 * Matches by identifier (no spaces, upper case) and amount; each file line matches only once.
 */
public class TransferReconciliation {

    public ReconciliationResult reconcile(List<TransferRecord> fromCerne,
                                          List<TransferRecord> fromFile) {
        List<TransferRecord> matched = new ArrayList<TransferRecord>();
        List<TransferRecord> onlyInCerne = new ArrayList<TransferRecord>();
        List<TransferRecord> onlyInFile = new ArrayList<TransferRecord>();
        boolean[] used = new boolean[fromFile.size()];

        for (int i = 0; i < fromCerne.size(); i++) {
            TransferRecord fromSystem = fromCerne.get(i);
            Integer position = findInFile(fromSystem, fromFile, used);
            if (position == null) {
                onlyInCerne.add(fromSystem);
            } else {
                used[position] = true;
                matched.add(fromSystem);
            }
        }

        for (int i = 0; i < fromFile.size(); i++) {
            if (!used[i]) {
                onlyInFile.add(fromFile.get(i));
            }
        }

        BigDecimal totalMatched = BigDecimal.ZERO;
        for (int i = 0; i < matched.size(); i++) {
            totalMatched = totalMatched.add(matched.get(i).getAmount());
        }

        Map<LocalDate, Integer> countByDate = new HashMap<LocalDate, Integer>();
        for (int i = 0; i < matched.size(); i++) {
            LocalDate date = matched.get(i).getDate();
            Integer current = countByDate.get(date);
            if (current == null) {
                countByDate.put(date, 1);
            } else {
                countByDate.put(date, current + 1);
            }
        }

        return new ReconciliationResult(matched, onlyInCerne, onlyInFile,
                totalMatched, countByDate);
    }

    /** Returns the position in the file, or null when not found. */
    private Integer findInFile(TransferRecord fromSystem, List<TransferRecord> fromFile,
                               boolean[] used) {
        for (int j = 0; j < fromFile.size(); j++) {
            if (used[j]) {
                continue;
            }
            TransferRecord candidate = fromFile.get(j);
            if (sameTransfer(fromSystem, candidate)) {
                return j;
            }
        }
        return null;
    }

    private boolean sameTransfer(TransferRecord a, TransferRecord b) {
        String idA = normalize(a.getIdentifier());
        String idB = normalize(b.getIdentifier());
        if (idA == null || idB == null) {
            return false;
        }
        if (!idA.equals(idB)) {
            return false;
        }
        if (a.getAmount() == null || b.getAmount() == null) {
            return false;
        }
        return a.getAmount().compareTo(b.getAmount()) == 0;
    }

    private String normalize(String identifier) {
        if (identifier == null) {
            return null;
        }
        return identifier.trim().toUpperCase();
    }
}
