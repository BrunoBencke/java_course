package cerne.statements;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;

import cerne.accounts.Branch;
import cerne.accounts.Account;
import cerne.members.Member;
import cerne.members.MemberRepository;
import cerne.entries.Entry;
import cerne.entries.EntryRepository;

/** No null: Optional as the return type and an empty list as "nothing" (session 2). */
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
                .map(Account::getBranch)
                .map(Branch::getName)
                .filter(name -> !name.isBlank())
                .orElse("Branch not provided");
        String memberName = member.map(Member::getName).orElse("");
        return "STATEMENT - " + branchName + " - " + memberName;
    }

    /** Empty list both for "member does not exist" and for "had no activity": new contract. */
    public List<Entry> entriesForPeriod(String cpf, LocalDate start, LocalDate end) {
        return members.findByCpf(cpf)
                .map(Member::getMainAccount)
                .map(account -> entries.findByAccount(account.getNumber(), start, end))
                .orElse(Collections.emptyList());
    }

    /** Receives the entries in chronological order; the statement shows the most recent first. */
    public Statement buildStatement(Account account, List<Entry> chronologicalEntries) {
        if (chronologicalEntries.isEmpty()) {
            return Statement.empty(account.getNumber());
        }
        Entry first = chronologicalEntries.get(0);
        Entry last = chronologicalEntries.get(chronologicalEntries.size() - 1);
        List<Entry> mostRecentFirst = new ArrayList<>(chronologicalEntries);
        Collections.reverse(mostRecentFirst);
        return new Statement(account.getNumber(), first.getDate(), last.getDate(), mostRecentFirst);
    }
}
