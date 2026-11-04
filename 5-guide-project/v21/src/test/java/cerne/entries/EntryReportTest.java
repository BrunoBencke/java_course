package cerne.entries;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cerne.accounts.Account;
import cerne.accounts.Branch;
import cerne.measurement.DatasetGenerator;
import cerne.members.Member;
import cerne.members.MemberType;

class EntryReportTest {

    private static final LocalDate START = LocalDate.of(2026, 8, 1);
    private static final LocalDate END = LocalDate.of(2026, 8, 31);

    private final EntryReport report = new EntryReport();
    private Member ana;
    private Member bruno;
    private Member carla;

    @BeforeEach
    void setUp() {
        ana = member("Ana", "0001-1", "0001-2");
        bruno = member("Bruno", "0002-1");
        carla = member("Carla", "0003-1");
    }

    private static Member member(String name, String... accounts) {
        var member = new Member(name, name, MemberType.INDIVIDUAL);
        for (String number : accounts) {
            member.addAccount(new Account(number, new Branch("0001", "Downtown"), member, BigDecimal.ZERO));
        }
        return member;
    }

    private static Entry credit(String account, int day, String amount) {
        return new Credit(account, new BigDecimal(amount), LocalDate.of(2026, 8, day), "");
    }

    @Test
    @DisplayName("sums credits, debits and fees across all of the member's accounts")
    void sumsAllAccounts() {
        var entries = List.of(
                credit("0001-1", 5, "100.00"), credit("0001-2", 6, "50.00"),
                new Debit("0001-1", new BigDecimal("30.00"), LocalDate.of(2026, 8, 7)),
                new Fee("0001-2", new BigDecimal("5.00"), LocalDate.of(2026, 8, 8), "TED"));

        var lines = report.generate(List.of(ana), entries, START, END);

        assertEquals(List.of(new ReportLine("Ana", 4, new BigDecimal("150.00"), new BigDecimal("35.00"))), lines);
    }

    @Test
    @DisplayName("a reversal counts as inflow")
    void reversalCountsAsInflow() {
        var reversal = new Reversal("0001-1", new BigDecimal("20.00"), LocalDate.of(2026, 8, 9),
                java.util.UUID.randomUUID());
        var lines = report.generate(List.of(ana), List.of(reversal), START, END);
        assertEquals(new BigDecimal("20.00"), lines.get(0).totalCredit());
    }

    @Test
    @DisplayName("sorts by credit descending and drops whoever had no activity")
    void sortsAndDrops() {
        var entries = List.of(credit("0002-1", 5, "300.00"), credit("0003-1", 5, "20.00"));
        var lines = report.generate(List.of(ana, bruno, carla), entries, START, END);
        assertEquals(List.of("Bruno", "Carla"), lines.stream().map(ReportLine::memberName).toList());
    }

    @Test
    @DisplayName("legacy version and Streams version give the same result")
    void legacyAndStreamsMatch() {
        var members = DatasetGenerator.members(300);
        var entries = DatasetGenerator.entries(members, 5_000);
        assertEquals(new LegacyEntryReport().generate(members, entries, START, END),
                report.generate(members, entries, START, END));
    }
}
