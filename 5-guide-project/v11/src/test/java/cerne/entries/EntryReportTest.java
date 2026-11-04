package cerne.entries;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import cerne.accounts.Branch;
import cerne.accounts.Account;
import cerne.members.Member;
import cerne.members.MemberType;
import cerne.measurement.DatasetGenerator;

public class EntryReportTest {

    private static final LocalDate START = LocalDate.of(2026, 8, 1);
    private static final LocalDate END = LocalDate.of(2026, 8, 31);

    private final EntryReport report = new EntryReport();
    private Member ana;
    private Member bruno;
    private Member carla;

    @Before
    public void setUp() {
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
        return new CreditEntry(account, new BigDecimal(amount), LocalDate.of(2026, 8, day));
    }

    private static Entry debit(String account, int day, String amount) {
        return new DebitEntry(account, new BigDecimal(amount), LocalDate.of(2026, 8, day));
    }

    private static Entry fee(String account, int day, String amount) {
        return new FeeEntry(account, new BigDecimal(amount), LocalDate.of(2026, 8, day));
    }

    @Test
    public void sumsCreditsAndDebitsOfAllMemberAccounts() {
        var entries = List.of(
                credit("0001-1", 5, "100.00"), credit("0001-2", 6, "50.00"),
                debit("0001-1", 7, "30.00"), fee("0001-2", 8, "5.00"));

        List<ReportLine> lines = report.generate(List.of(ana), entries, START, END);

        assertEquals(1, lines.size());
        assertEquals(new ReportLine("Ana", 4, new BigDecimal("150.00"), new BigDecimal("35.00")), lines.get(0));
    }

    @Test
    public void sortsByDescendingCredit() {
        var entries = List.of(
                credit("0001-1", 5, "10.00"), credit("0002-1", 5, "300.00"), credit("0003-1", 5, "20.00"));

        List<ReportLine> lines = report.generate(List.of(ana, bruno, carla), entries, START, END);

        assertEquals("Bruno", lines.get(0).getMemberName());
        assertEquals("Carla", lines.get(1).getMemberName());
        assertEquals("Ana", lines.get(2).getMemberName());
    }

    @Test
    public void memberWithoutActivityDoesNotAppearInReport() {
        List<ReportLine> lines = report.generate(List.of(ana, bruno),
                List.of(credit("0002-1", 5, "10.00")), START, END);
        assertEquals(1, lines.size());
        assertEquals("Bruno", lines.get(0).getMemberName());
    }

    @Test
    public void periodBoundsAreInclusive() {
        var entries = List.of(
                credit("0001-1", 1, "10.00"), credit("0001-1", 31, "20.00"),
                new CreditEntry("0001-1", new BigDecimal("99.00"), LocalDate.of(2026, 9, 1)));

        List<ReportLine> lines = report.generate(List.of(ana), entries, START, END);

        assertEquals(2, lines.get(0).getCount());
        assertEquals(new BigDecimal("30.00"), lines.get(0).getTotalCredit());
    }

    @Test
    public void differentScaleDoesNotChangeTotals() {
        List<ReportLine> lines = report.generate(List.of(ana),
                List.of(credit("0001-1", 5, "150.0")), START, END);
        assertEquals(new BigDecimal("150.00"), lines.get(0).getTotalCredit());
    }

    @Test
    public void formatsOneLinePerMember() {
        List<ReportLine> lines = report.generate(List.of(ana, bruno),
                List.of(credit("0001-1", 5, "10.00"), debit("0002-1", 5, "4.00")), START, END);
        assertEquals("Ana;1;10.00;0.00\nBruno;1;0.00;4.00\n", report.format(lines));
    }

    @Test
    public void legacyVersionAndStreamsVersionGiveTheSameResult() {
        List<Member> members = DatasetGenerator.members(300);
        List<Entry> entries = DatasetGenerator.entries(members, 5_000);
        assertEquals(new LegacyEntryReport().generate(members, entries, START, END),
                report.generate(members, entries, START, END));
    }
}
