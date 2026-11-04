package cerne.statements;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import cerne.accounts.Account;
import cerne.accounts.Branch;
import cerne.entries.Entry;
import cerne.entries.EntryRepository;
import cerne.members.Member;
import cerne.members.MemberRepository;

@Stateless
public class StatementService {

    @Inject
    private MemberRepository members;

    @Inject
    private EntryRepository entries;

    public StatementService() {
    }

    public StatementService(MemberRepository members, EntryRepository entries) {
        this.members = members;
        this.entries = entries;
    }

    public String statementHeader(String cpf) {
        Optional<Member> member = members.findByCpf(cpf);
        String branchName = member
                .map(Member::getMainAccount)
                .map(Account::branch)
                .map(Branch::name)
                .filter(name -> !name.isBlank())
                .orElse("Branch not provided");
        String memberName = member.map(Member::getName).orElse("");
        return "STATEMENT - " + branchName + " - " + memberName;
    }

    public List<Entry> entriesForPeriod(String cpf, LocalDate start, LocalDate end) {
        return members.findByCpf(cpf)
                .map(Member::getMainAccount)
                .map(account -> entries.findByAccount(account.number(), start, end))
                .orElse(List.of());
    }

    /**
     * Receives the entries in chronological order; the statement shows the most recent first.
     * Sequenced collections (JEP 431): reversed() is a view; the statement keeps an immutable copy.
     */
    public Statement buildStatement(Account account, List<Entry> chronologicalEntries) {
        if (chronologicalEntries.isEmpty()) {
            return Statement.empty(account.number());
        }
        return new Statement(account.number(),
                chronologicalEntries.getFirst().date(),
                chronologicalEntries.getLast().date(),
                List.copyOf(chronologicalEntries.reversed()));
    }
}
