package cerne.statements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

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

    // SMELL 1: cascading nulls and return null as a business result
    public String statementHeader(String cpf) {
        Member member = members.findByCpf(cpf);
        String branchName = "";
        if (member != null) {
            Account account = member.getMainAccount();
            if (account != null) {
                Branch branch = account.getBranch();
                if (branch != null) {
                    if (branch.getName() != null) {
                        branchName = branch.getName();
                    }
                }
            }
        }
        if (branchName.equals("")) {
            branchName = "BRANCH NOT PROVIDED";
        }
        String memberName = member == null ? "" : member.getName();
        return "STATEMENT - " + branchName + " - " + memberName;
    }

    public List<Entry> entriesForPeriod(String cpf, Date start, Date end) {
        Member member = members.findByCpf(cpf);
        if (member == null) {
            return null;
        }
        List<Entry> result = entries.findByAccount(
                member.getMainAccount().getNumber(), start, end);
        if (result == null) {
            return null;
        }
        return result;
    }

    /** Receives the entries in chronological order; the statement shows the most recent first. */
    public Statement buildStatement(Account account, List<Entry> chronologicalEntries) {
        if (chronologicalEntries.isEmpty()) {
            return Statement.empty(account.getNumber());
        }
        Entry first = chronologicalEntries.get(0);
        Entry last = chronologicalEntries.get(chronologicalEntries.size() - 1);
        List<Entry> mostRecentFirst = new ArrayList<Entry>(chronologicalEntries);
        Collections.reverse(mostRecentFirst);
        return new Statement(account.getNumber(), first.getDate(), last.getDate(), mostRecentFirst);
    }
}
