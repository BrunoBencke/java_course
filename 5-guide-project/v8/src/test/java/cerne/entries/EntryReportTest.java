package cerne.entries;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import cerne.accounts.Account;
import cerne.accounts.Branch;
import cerne.members.Member;
import cerne.members.MemberType;
import cerne.util.CerneUtil;

public class EntryReportTest {

    private static final Date START = CerneUtil.date(2026, 8, 1);
    private static final Date END = CerneUtil.date(2026, 8, 31);

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
        Member member = new Member(name, name, MemberType.INDIVIDUAL);
        for (String number : accounts) {
            member.addAccount(new Account(number, new Branch("0001", "Downtown"), member, BigDecimal.ZERO));
        }
        return member;
    }

    private static Entry credit(String account, int day, double amount) {
        return new CreditEntry(account, amount, CerneUtil.date(2026, 8, day));
    }

    private static Entry debit(String account, int day, double amount) {
        return new DebitEntry(account, amount, CerneUtil.date(2026, 8, day));
    }

    private static Entry fee(String account, int day, double amount) {
        return new FeeEntry(account, amount, CerneUtil.date(2026, 8, day));
    }

    @Test
    public void sumsCreditsAndDebitsOfAllMemberAccounts() {
        List<Entry> entries = Arrays.asList(
                credit("0001-1", 5, 100.00), credit("0001-2", 6, 50.00),
                debit("0001-1", 7, 30.00), fee("0001-2", 8, 5.00));

        List<ReportLine> lines = report.generate(Arrays.asList(ana), entries, START, END);

        assertEquals(1, lines.size());
        assertEquals(new ReportLine("Ana", 4, 150.00, 35.00), lines.get(0));
    }

    @Test
    public void sortsByDescendingCredit() {
        List<Entry> entries = Arrays.asList(
                credit("0001-1", 5, 10.00), credit("0002-1", 5, 300.00), credit("0003-1", 5, 20.00));

        List<ReportLine> lines = report.generate(Arrays.asList(ana, bruno, carla), entries, START, END);

        assertEquals("Bruno", lines.get(0).getMemberName());
        assertEquals("Carla", lines.get(1).getMemberName());
        assertEquals("Ana", lines.get(2).getMemberName());
    }

    @Test
    public void memberWithoutActivityDoesNotAppearInReport() {
        List<ReportLine> lines = report.generate(Arrays.asList(ana, bruno),
                Arrays.asList(credit("0002-1", 5, 10.00)), START, END);
        assertEquals(1, lines.size());
        assertEquals("Bruno", lines.get(0).getMemberName());
    }

    @Test
    public void periodBoundsAreInclusive() {
        List<Entry> entries = Arrays.asList(
                credit("0001-1", 1, 10.00), credit("0001-1", 31, 20.00),
                new CreditEntry("0001-1", 99.00, CerneUtil.date(2026, 9, 1)));

        List<ReportLine> lines = report.generate(Arrays.asList(ana), entries, START, END);

        assertEquals(2, lines.get(0).getCount());
        assertEquals(30.00, lines.get(0).getTotalCredit(), 0.001);
    }

    @Test
    public void formatsOneLinePerMember() {
        List<ReportLine> lines = report.generate(Arrays.asList(ana, bruno),
                Arrays.asList(credit("0001-1", 5, 10.00), debit("0002-1", 5, 4.00)), START, END);
        assertEquals("Ana;1;10.0;0.0\nBruno;1;0.0;4.0\n", report.format(lines));
    }
}
