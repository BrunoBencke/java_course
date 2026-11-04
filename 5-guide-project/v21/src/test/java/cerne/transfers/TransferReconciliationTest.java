package cerne.transfers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The 14 tests of the session 2 challenge, now in JUnit 5 (same assertions, record accessors). */
class TransferReconciliationTest {

    private static final LocalDate DAY_1 = LocalDate.of(2026, 8, 3);
    private static final LocalDate DAY_2 = LocalDate.of(2026, 8, 4);

    private TransferReconciliation reconciliation;

    @BeforeEach
    void setUp() {
        reconciliation = new TransferReconciliation();
    }

    private static TransferRecord record(String id, String amount, LocalDate date) {
        return new TransferRecord(id, new BigDecimal(amount), date);
    }

    private static List<TransferRecord> list(TransferRecord... records) {
        return List.of(records);
    }

    @Test
    void noRecordsMeansNothingToReconcile() {
        ReconciliationResult r = reconciliation.reconcile(List.of(),
                List.of());
        assertTrue(r.matched().isEmpty());
        assertTrue(r.onlyInCerne().isEmpty());
        assertTrue(r.onlyInFile().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.totalMatched()));
    }

    @Test
    void sameTransferOnBothSidesIsMatched() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-1", "10.00", DAY_1)),
                list(record("TRF-1", "10.00", DAY_1)));
        assertEquals("TRF-1", ReconciliationResult.identifiers(r.matched()));
        assertTrue(r.onlyInCerne().isEmpty());
        assertTrue(r.onlyInFile().isEmpty());
    }

    @Test
    void transferMissingFromFileStaysOnlyInCerne() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-1", "10.00", DAY_1)),
                List.of());
        assertEquals("TRF-1", ReconciliationResult.identifiers(r.onlyInCerne()));
        assertTrue(r.matched().isEmpty());
    }

    @Test
    void fileLineWithoutCounterpartStaysOnlyInFile() {
        ReconciliationResult r = reconciliation.reconcile(List.of(),
                list(record("TRF-9", "5.00", DAY_1)));
        assertEquals("TRF-9", ReconciliationResult.identifiers(r.onlyInFile()));
    }

    @Test
    void mismatchedAmountAppearsOnBothSides() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-1", "10.00", DAY_1)),
                list(record("TRF-1", "11.00", DAY_1)));
        assertTrue(r.matched().isEmpty());
        assertEquals("TRF-1", ReconciliationResult.identifiers(r.onlyInCerne()));
        assertEquals("TRF-1", ReconciliationResult.identifiers(r.onlyInFile()));
    }

    @Test
    void differentScaleOfSameAmountMatches() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-1", "10.0", DAY_1)),
                list(record("TRF-1", "10.00", DAY_1)));
        assertEquals(1, r.matched().size());
    }

    @Test
    void fileIdentifierIsNormalized() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-1", "10.00", DAY_1)),
                list(record("  trf-1 ", "10.00", DAY_1)));
        assertEquals(1, r.matched().size());
        assertTrue(r.onlyInFile().isEmpty());
    }

    @Test
    void matchedFollowCerneOrder() {
        ReconciliationResult r = reconciliation.reconcile(
                list(record("TRF-2", "2.00", DAY_1), record("TRF-1", "1.00", DAY_1), record("TRF-3", "3.00", DAY_1)),
                list(record("TRF-1", "1.00", DAY_1), record("TRF-3", "3.00", DAY_1), record("TRF-2", "2.00", DAY_1)));
        assertEquals("TRF-2,TRF-1,TRF-3", ReconciliationResult.identifiers(r.matched()));
    }

    @Test
    void onlyInFileFollowsFileOrder() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-2", "2.00", DAY_1)),
                list(record("TRF-5", "5.00", DAY_1), record("TRF-2", "2.00", DAY_1), record("TRF-4", "4.00", DAY_1)));
        assertEquals("TRF-5,TRF-4", ReconciliationResult.identifiers(r.onlyInFile()));
    }

    @Test
    void mixedScenarioSeparatesTheThreeLists() {
        ReconciliationResult r = reconciliation.reconcile(
                list(record("TRF-1", "1.00", DAY_1), record("TRF-2", "2.00", DAY_1), record("TRF-3", "3.00", DAY_2)),
                list(record("TRF-3", "3.00", DAY_2), record("TRF-4", "4.00", DAY_2), record("TRF-1", "1.00", DAY_1)));
        assertEquals("TRF-1,TRF-3", ReconciliationResult.identifiers(r.matched()));
        assertEquals("TRF-2", ReconciliationResult.identifiers(r.onlyInCerne()));
        assertEquals("TRF-4", ReconciliationResult.identifiers(r.onlyInFile()));
    }

    @Test
    void totalMatchedSumsOnlyTheMatched() {
        ReconciliationResult r = reconciliation.reconcile(
                list(record("TRF-1", "10.50", DAY_1), record("TRF-2", "99.99", DAY_1), record("TRF-3", "0.50", DAY_1)),
                list(record("TRF-1", "10.50", DAY_1), record("TRF-3", "0.50", DAY_1)));
        assertEquals(0, new BigDecimal("11.00").compareTo(r.totalMatched()));
    }

    @Test
    void duplicateLineInFileMatchesOnlyOnce() {
        ReconciliationResult r = reconciliation.reconcile(list(record("TRF-1", "10.00", DAY_1)),
                list(record("TRF-1", "10.00", DAY_1), record("TRF-1", "10.00", DAY_1)));
        assertEquals(1, r.matched().size());
        assertEquals("TRF-1", ReconciliationResult.identifiers(r.onlyInFile()));
    }

    @Test
    void duplicateInCerneWithOneFileLineLeavesOneInCerne() {
        ReconciliationResult r = reconciliation.reconcile(
                list(record("TRF-1", "10.00", DAY_1), record("TRF-1", "10.00", DAY_1)),
                list(record("TRF-1", "10.00", DAY_1)));
        assertEquals(1, r.matched().size());
        assertEquals(1, r.onlyInCerne().size());
    }

    @Test
    void countByDateCountsTheMatchedOfEachDay() {
        ReconciliationResult r = reconciliation.reconcile(
                list(record("TRF-1", "1.00", DAY_1), record("TRF-2", "2.00", DAY_1), record("TRF-3", "3.00", DAY_2),
                        record("TRF-4", "4.00", DAY_2)),
                list(record("TRF-1", "1.00", DAY_1), record("TRF-2", "2.00", DAY_1), record("TRF-3", "3.00", DAY_2)));
        assertEquals(Integer.valueOf(2), r.countByDate().get(DAY_1));
        assertEquals(Integer.valueOf(1), r.countByDate().get(DAY_2));
        assertEquals(2, r.countByDate().size());
    }
}
