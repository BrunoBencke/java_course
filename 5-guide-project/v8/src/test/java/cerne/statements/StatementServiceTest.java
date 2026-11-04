package cerne.statements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import cerne.accounts.Account;
import cerne.accounts.Branch;
import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.Entry;
import cerne.entries.InMemoryEntryRepository;
import cerne.members.InMemoryMemberRepository;
import cerne.members.Member;
import cerne.members.MemberType;
import cerne.util.CerneUtil;

public class StatementServiceTest {

    private InMemoryMemberRepository members;
    private InMemoryEntryRepository entries;
    private StatementService service;
    private Account account;

    @Before
    public void setUp() {
        members = new InMemoryMemberRepository();
        entries = new InMemoryEntryRepository();
        service = new StatementService(members, entries);
        Member ana = new Member("52998224725", "Ana", MemberType.INDIVIDUAL);
        account = new Account("0001-1", new Branch("0001", "Downtown Branch"), ana, BigDecimal.ZERO);
        ana.addAccount(account);
        members.save(ana);
    }

    @Test
    public void headerShowsBranchAndMember() {
        assertEquals("STATEMENT - Downtown Branch - Ana", service.statementHeader("52998224725"));
    }

    @Test
    public void headerWithoutBranchUsesDefaultValue() {
        account.setBranch(null);
        assertEquals("STATEMENT - BRANCH NOT PROVIDED - Ana", service.statementHeader("52998224725"));
    }

    @Test
    public void unknownMemberReturnsNull() {
        assertNull(service.entriesForPeriod("00000000000", CerneUtil.date(2026, 8, 1), CerneUtil.date(2026, 8, 31)));
    }

    @Test
    public void entriesForPeriodOfMainAccount() {
        entries.save(new CreditEntry("0001-1", 10.0, CerneUtil.date(2026, 8, 5)));
        entries.save(new CreditEntry("0001-1", 20.0, CerneUtil.date(2026, 9, 5)));
        assertEquals(1, service.entriesForPeriod("52998224725",
                CerneUtil.date(2026, 8, 1), CerneUtil.date(2026, 8, 31)).size());
    }

    @Test
    public void statementShowsMostRecentFirst() {
        List<Entry> chronological = Arrays.<Entry>asList(
                new CreditEntry("0001-1", 10.0, CerneUtil.date(2026, 8, 1)),
                new DebitEntry("0001-1", 5.0, CerneUtil.date(2026, 8, 2)),
                new CreditEntry("0001-1", 7.0, CerneUtil.date(2026, 8, 3)));
        Statement statement = service.buildStatement(account, chronological);
        assertEquals(CerneUtil.date(2026, 8, 1), statement.getStart());
        assertEquals(CerneUtil.date(2026, 8, 3), statement.getEnd());
        assertEquals(7.0, statement.getEntries().get(0).getAmount(), 0.001);
    }
}
