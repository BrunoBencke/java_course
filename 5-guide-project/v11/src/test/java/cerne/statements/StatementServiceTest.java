package cerne.statements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import cerne.accounts.Branch;
import cerne.accounts.Account;
import cerne.members.Member;
import cerne.members.InMemoryMemberRepository;
import cerne.members.MemberType;
import cerne.entries.Entry;
import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.InMemoryEntryRepository;

public class StatementServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 8, 1);
    private static final LocalDate END = LocalDate.of(2026, 8, 31);

    private InMemoryEntryRepository entries;
    private StatementService service;
    private Account account;

    @Before
    public void setUp() {
        var members = new InMemoryMemberRepository();
        entries = new InMemoryEntryRepository();
        service = new StatementService(members, entries);
        var ana = new Member("52998224725", "Ana", MemberType.INDIVIDUAL);
        account = new Account("0001-1", new Branch("0001", "Downtown Branch"), ana, BigDecimal.ZERO);
        ana.addAccount(account);
        members.save(ana);
    }

    private static Entry credit(String amount, LocalDate date) {
        return new CreditEntry("0001-1", new BigDecimal(amount), date);
    }

    @Test
    public void headerShowsBranchAndMember() {
        assertEquals("STATEMENT - Downtown Branch - Ana", service.statementHeader("52998224725"));
    }

    @Test
    public void headerWithoutBranchUsesDefaultValue() {
        account.setBranch(null);
        assertEquals("STATEMENT - Branch not provided - Ana", service.statementHeader("52998224725"));
    }

    @Test
    public void unknownMemberReturnsEmptyList() {
        // contract change: in v8 it was null
        assertTrue(service.entriesForPeriod("00000000000", START, END).isEmpty());
    }

    @Test
    public void entriesForPeriodOfMainAccount() {
        entries.save(credit("10.00", LocalDate.of(2026, 8, 5)));
        entries.save(credit("20.00", LocalDate.of(2026, 9, 5)));
        assertEquals(1, service.entriesForPeriod("52998224725", START, END).size());
    }

    @Test
    public void statementShowsMostRecentFirst() {
        List<Entry> chronological = List.of(
                credit("10.00", LocalDate.of(2026, 8, 1)),
                new DebitEntry("0001-1", new BigDecimal("5.00"), LocalDate.of(2026, 8, 2)),
                credit("7.00", LocalDate.of(2026, 8, 3)));
        Statement statement = service.buildStatement(account, chronological);
        assertEquals(LocalDate.of(2026, 8, 1), statement.getStart());
        assertEquals(LocalDate.of(2026, 8, 3), statement.getEnd());
        assertEquals(new BigDecimal("7.00"), statement.getEntries().get(0).getAmount());
    }
}
