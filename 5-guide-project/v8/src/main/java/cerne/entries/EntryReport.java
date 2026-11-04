package cerne.entries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import cerne.accounts.Account;
import cerne.members.Member;

/** Report of entries per member in the period. */
public class EntryReport {

    // SMELL 4: quadratic nested loops (the whole entry list for each account) and String += in a loop
    public List<ReportLine> generate(List<Member> members, List<Entry> entries,
                                     Date start, Date end) {
        List<ReportLine> lines = new ArrayList<ReportLine>();
        for (Member member : members) {
            double totalCredit = 0;
            double totalDebit = 0;
            int count = 0;
            for (Account account : member.getAccounts()) {
                for (Entry entry : entries) {
                    if (entry.getAccountNumber().equals(account.getNumber())) {
                        if (!entry.getDate().before(start) && !entry.getDate().after(end)) {
                            if (Entry.CREDIT.equals(entry.getType())) {
                                totalCredit = totalCredit + entry.getAmount();
                            } else if (Entry.DEBIT.equals(entry.getType())
                                    || Entry.FEE.equals(entry.getType())) {
                                totalDebit = totalDebit + entry.getAmount();
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
                return Double.compare(b.getTotalCredit(), a.getTotalCredit());
            }
        });
        return lines;
    }

    /** Text for printing at the branch. */
    public String format(List<ReportLine> lines) {
        String text = "";
        for (ReportLine line : lines) {
            text += line.getMemberName() + ";" + line.getCount() + ";"
                    + line.getTotalCredit() + ";" + line.getTotalDebit() + "\n";
        }
        return text;
    }
}
