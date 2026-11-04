package cerne.transfers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/** The 14 tests of the session 2 challenge: the refactoring cannot change any assert. */
public class TransferReconciliationTest {

    private static final LocalDate DAY_1 = LocalDate.of(2026, 8, 3);
    private static final LocalDate DAY_2 = LocalDate.of(2026, 8, 4);

    private TransferReconciliation reconciliation;

    @Before
    public void setUp() {
        reconciliation = new TransferReconciliation();
    }

    private static TransferRecord record(String id, String amount, LocalDate date) {
        return new TransferRecord(id, new BigDecimal(amount), date);
    }

    private static List<TransferRecord> list(TransferRecord... records) {
        return new ArrayList<TransferRecord>(Arrays.asList(records));
    }

    @Test
    public void noRecordsMeansNothingToReconcile() {
        ReconciliationResult r = reconciliation.reconcile(Collections.<TransferRecord>emptyList(),
                Collections.<TransferRecord>emptyList());
        assertTrue(r.getMatched().isEmpty());
        assertTrue(r.getOnlyInCerne().isEmpty());
        assertTrue(r.getOnlyInFile().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.getTotalMatched()));
    }

    @Test
    public void sameTransferOnBothSidesIsMatched() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-1", "10.00", DAY_1)),
                list(record("TRF-1", "10.00", DAY_1)));
        assertEquals("TRF-1", ReconciliationResult.identifiers(r.getMatched()));
        assertTrue(r.getOnlyInCerne().isEmpty());
        assertTrue(r.getOnlyInFile().isEmpty());
    }

    @Test
    public void transferMissingFromFileStaysOnlyInCerne() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-1", "10.00", DAY_1)),
                Collections.<TransferRecord>emptyList());
        assertEquals("TRF-1", ReconciliationResult.identifiers(r.getOnlyInCerne()));
        assertTrue(r.getMatched().isEmpty());
    }

    @Test
    public void fileLineWithoutCounterpartStaysOnlyInFile() {
        ReconciliationResult r = reconciliation.reconcile(Collections.<TransferRecord>emptyList(),
                list(record("TRF-9", "5.00", DAY_1)));
        assertEquals("TRF-9", ReconciliationResult.identifiers(r.getOnlyInFile()));
    }

    @Test
    public void mismatchedAmountAppearsOnBothSides() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-1", "10.00", DAY_1)),
                list(record("TRF-1", "11.00", DAY_1)));
        assertTrue(r.getMatched().isEmpty());
        assertEquals("TRF-1", ReconciliationResult.identifiers(r.getOnlyInCerne()));
        assertEquals("TRF-1", ReconciliationResult.identifiers(r.getOnlyInFile()));
    }

    @Test
    public void differentScaleOfSameAmountMatches() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-1", "10.0", DAY_1)),
                list(record("TRF-1", "10.00", DAY_1)));
        assertEquals(1, r.getMatched().size());
    }

    @Test
    public void fileIdentifierIsNormalized() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-1", "10.00", DAY_1)),
                list(record("  trf-1 ", "10.00", DAY_1)));
        assertEquals(1, r.getMatched().size());
        assertTrue(r.getOnlyInFile().isEmpty());
    }

    @Test
    public void matchedFollowCerneOrder() {
        ReconciliationResult r = reconciliation.reconcile(
                list(record("TRF-2", "2.00", DAY_1), record("TRF-1", "1.00", DAY_1), record("TRF-3", "3.00", DAY_1)),
                list(record("TRF-1", "1.00", DAY_1), record("TRF-3", "3.00", DAY_1), record("TRF-2", "2.00", DAY_1)));
        assertEquals("TRF-2,TRF-1,TRF-3", ReconciliationResult.identifiers(r.getMatched()));
    }

    @Test
    public void onlyInFileFollowsFileOrder() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-2", "2.00", DAY_1)),
                list(record("TRF-5", "5.00", DAY_1), record("TRF-2", "2.00", DAY_1), record("TRF-4", "4.00", DAY_1)));
        assertEquals("TRF-5,TRF-4", ReconciliationResult.identifiers(r.getOnlyInFile()));
    }

    @Test
    public void mixedScenarioSeparatesTheThreeLists() {
        ReconciliationResult r = reconciliation.reconcile(
                list(record("TRF-1", "1.00", DAY_1), record("TRF-2", "2.00", DAY_1), record("TRF-3", "3.00", DAY_2)),
                list(record("TRF-3", "3.00", DAY_2), record("TRF-4", "4.00", DAY_2), record("TRF-1", "1.00", DAY_1)));
        assertEquals("TRF-1,TRF-3", ReconciliationResult.identifiers(r.getMatched()));
        assertEquals("TRF-2", ReconciliationResult.identifiers(r.getOnlyInCerne()));
        assertEquals("TRF-4", ReconciliationResult.identifiers(r.getOnlyInFile()));
    }

    @Test
    public void totalMatchedSumsOnlyTheMatched() {
        ReconciliationResult r = reconciliation.reconcile(
                list(record("TRF-1", "10.50", DAY_1), record("TRF-2", "99.99", DAY_1), record("TRF-3", "0.50", DAY_1)),
                list(record("TRF-1", "10.50", DAY_1), record("TRF-3", "0.50", DAY_1)));
        assertEquals(0, new BigDecimal("11.00").compareTo(r.getTotalMatched()));
    }

    @Test
    public void duplicateLineInFileMatchesOnlyOnce() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-1", "10.00", DAY_1)),
                list(record("TRF-1", "10.00", DAY_1), record("TRF-1", "10.00", DAY_1)));
        assertEquals(1, r.getMatched().size());
        assertEquals("TRF-1", ReconciliationResult.identifiers(r.getOnlyInFile()));
    }

    @Test
    public void duplicateInCerneWithOneFileLineLeavesOneInCerne() {
        ReconciliationResult r = reconciliation.reconcile(
                list(record("TRF-1", "10.00", DAY_1), record("TRF-1", "10.00", DAY_1)),
                list(record("TRF-1", "10.00", DAY_1)));
        assertEquals(1, r.getMatched().size());
        assertEquals(1, r.getOnlyInCerne().size());
    }

    @Test
    public void countByDateCountsTheMatchedOfEachDay() {
        ReconciliationResult r = reconciliation.reconcile(
                list(record("TRF-1", "1.00", DAY_1), record("TRF-2", "2.00", DAY_1), record("TRF-3", "3.00", DAY_2),
                        record("TRF-4", "4.00", DAY_2)),
                list(record("TRF-1", "1.00", DAY_1), record("TRF-2", "2.00", DAY_1), record("TRF-3", "3.00", DAY_2)));
        assertEquals(Integer.valueOf(2), r.getCountByDate().get(DAY_1));
        assertEquals(Integer.valueOf(1), r.getCountByDate().get(DAY_2));
        assertEquals(2, r.getCountByDate().size());
    }
}
