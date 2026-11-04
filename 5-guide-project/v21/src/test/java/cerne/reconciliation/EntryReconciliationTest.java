package cerne.reconciliation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Covers the module's four classes; the session 5 challenge replaces the instanceof without touching these tests. */
class EntryReconciliationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 10);

    private static InternalEntry internal(String id, String amount) {
        return new InternalEntry(id, new BigDecimal(amount), LocalDate.of(2026, 8, 3));
    }

    private static ExternalRecord external(String id, String amount) {
        return new ExternalRecord(id, new BigDecimal(amount), LocalDate.of(2026, 8, 3));
    }

    private static final Matched MATCHED = new Matched(internal("L1", "10.00"), external("L1", "10.00"));
    private static final Pending PENDING = new Pending(internal("L2", "20.00"), 7);
    private static final Mismatched ROUNDING = new Mismatched(internal("L3", "30.00"), external("L3", "29.97"));
    private static final Mismatched MISMATCHED = new Mismatched(internal("L4", "300.00"), external("L4", "150.00"));

    @Test
    @DisplayName("reconciler separates matched, pending and mismatched")
    void reconcilerSeparatesTheThreeCases() {
        var reconciler = new EntryReconciler();
        var outcomes = reconciler.reconcile(
                List.of(internal("L1", "10.00"), internal("L2", "20.00"), internal("L4", "300.00")),
                List.of(external("L1", "10.00"), external("L4", "150.00")), TODAY);
        assertInstanceOf(Matched.class, outcomes.get(0));
        assertEquals(new Pending(internal("L2", "20.00"), 7), outcomes.get(1));
        assertInstanceOf(Mismatched.class, outcomes.get(2));
        assertEquals(1, reconciler.pendingCount(outcomes));
        assertEquals(new BigDecimal("150.00"), reconciler.totalDifference(outcomes));
    }

    @Test
    @DisplayName("report describes each case, with a rounding tolerance")
    void reportDescribesEachCase() {
        var generator = new ReconciliationReportGenerator();
        assertEquals("OK L1", generator.describe(MATCHED));
        assertEquals("PENDING L2 for 7 days", generator.describe(PENDING));
        assertEquals("ROUNDING L3", generator.describe(ROUNDING));
        assertEquals("MISMATCHED L4 difference 150.00", generator.describe(MISMATCHED));
    }

    @ParameterizedTest(name = "{0} has severity {1}")
    @CsvSource({"matched, NONE", "pending, HIGH", "rounding, LOW", "mismatched, HIGH"})
    @DisplayName("discrepancy classifier")
    void classifier(String scenario, DiscrepancySeverity expected) {
        var outcome = switch (scenario) {
            case "matched" -> MATCHED;
            case "pending" -> PENDING;
            case "rounding" -> ROUNDING;
            default -> MISMATCHED;
        };
        assertEquals(expected, new DiscrepancyClassifier().classify(outcome));
    }

    @Test
    @DisplayName("exporter generates one CSV line per outcome")
    void exporter() {
        var exporter = new ReconciliationExporter();
        assertEquals("L1;MATCHED;10.00;10.00", exporter.csvLine(MATCHED));
        assertEquals("L2;PENDING;20.00;", exporter.csvLine(PENDING));
        assertEquals("L4;MISMATCHED;300.00;150.00", exporter.csvLine(MISMATCHED));
    }
}
