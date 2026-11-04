package cerne.statements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cerne.accounts.Account;
import cerne.accounts.Branch;
import cerne.entries.Credit;
import cerne.entries.Debit;
import cerne.entries.Entry;
import cerne.entries.InMemoryEntryRepository;
import cerne.members.InMemoryMemberRepository;
import cerne.members.Member;
import cerne.members.MemberType;

class StatementServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 8, 1);
    private static final LocalDate END = LocalDate.of(2026, 8, 31);

    private InMemoryEntryRepository entries;
    private StatementService service;
    private Account account;

    @BeforeEach
    void setUp() {
        var members = new InMemoryMemberRepository();
        entries = new InMemoryEntryRepository();
        service = new StatementService(members, entries);
        var ana = new Member("52998224725", "Ana", MemberType.INDIVIDUAL);
        account = new Account("0001-1", new Branch("0001", "Downtown Branch"), ana, BigDecimal.ZERO);
        ana.addAccount(account);
        members.save(ana);
    }

    private static Entry credit(String amount, LocalDate date) {
        return new Credit("0001-1", new BigDecimal(amount), date, "");
    }

    @Test
    void headerShowsBranchAndMember() {
        assertEquals("STATEMENT - Downtown Branch - Ana", service.statementHeader("52998224725"));
    }

    @Test
    void headerWithoutBranchUsesDefaultValue() {
        account.changeBranch(null);
        assertEquals("STATEMENT - Branch not provided - Ana", service.statementHeader("52998224725"));
    }

    @Test
    void unknownMemberReturnsEmptyList() {
        assertTrue(service.entriesForPeriod("00000000000", START, END).isEmpty());
    }

    @Test
    @DisplayName("statement: first and last by index, reversed copy (21 replaces them with getFirst/getLast/reversed)")
    void statementShowsMostRecentFirst() {
        List<Entry> chronological = List.of(
                credit("10.00", LocalDate.of(2026, 8, 1)),
                new Debit("0001-1", new BigDecimal("5.00"), LocalDate.of(2026, 8, 2)),
                credit("7.00", LocalDate.of(2026, 8, 3)));
        var statement = service.buildStatement(account, chronological);
        assertEquals(LocalDate.of(2026, 8, 1), statement.start());
        assertEquals(LocalDate.of(2026, 8, 3), statement.end());
        assertEquals(new BigDecimal("7.00"), statement.entries().get(0).amount());
    }
}
