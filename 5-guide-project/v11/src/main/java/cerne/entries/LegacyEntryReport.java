package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cerne.accounts.Account;
import cerne.members.Member;

/** Version with nested loops, kept only for the before/after measurement (ReportBenchmark). */
public class LegacyEntryReport {

    public List<ReportLine> generate(List<Member> members, List<Entry> entries,
                                     LocalDate start, LocalDate end) {
        List<ReportLine> lines = new ArrayList<ReportLine>();
        for (Member member : members) {
            BigDecimal totalCredit = BigDecimal.ZERO;
            BigDecimal totalDebit = BigDecimal.ZERO;
            int count = 0;
            for (Account account : member.getAccounts()) {
                for (Entry entry : entries) {
                    if (entry.getAccountNumber().equals(account.getNumber())) {
                        if (!entry.getDate().isBefore(start) && !entry.getDate().isAfter(end)) {
                            if (entry.getType() == EntryType.CREDIT) {
                                totalCredit = totalCredit.add(entry.getAmount());
                            } else if (entry.getType() == EntryType.DEBIT
                                    || entry.getType() == EntryType.FEE) {
                                totalDebit = totalDebit.add(entry.getAmount());
                            }
                            count++;
                        }
                    }
                }
            }
            if (count > 0) {
                lines.add(new ReportLine(member.getName(), count, totalCredit, totalDebit));
            }
        }
        Collections.sort(lines, new Comparator<ReportLine>() {
            @Override
            public int compare(ReportLine a, ReportLine b) {
                return b.getTotalCredit().compareTo(a.getTotalCredit());
            }
        });
        return lines;
    }
}
