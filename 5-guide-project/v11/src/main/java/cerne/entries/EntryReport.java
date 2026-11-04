package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cerne.accounts.Account;
import cerne.members.Member;

/** Per-member report rewritten with Streams (session 2): index, line per member, final pipeline. */
public class EntryReport {

    public List<ReportLine> generate(List<Member> members, List<Entry> entries,
                                     LocalDate start, LocalDate end) {
        // Step 1: index only once (the gain comes from here, not from the Streams)
        var entriesByAccount = entries.stream()
                .filter(entry -> entry.isBetween(start, end))
                .collect(Collectors.groupingBy(Entry::getAccountNumber));

        // Step 3: index; build the line; drop those with no activity; sort; return
        return members.stream()
                .map(member -> lineFor(member, entriesByAccount))
                .filter(line -> line.getCount() > 0)
                .sorted(Comparator.comparing(ReportLine::getTotalCredit).reversed())
                .collect(Collectors.toList());
    }

    // Step 2: one member's line from the index
    private ReportLine lineFor(Member member, Map<String, List<Entry>> entriesByAccount) {
        List<Entry> ofMember = member.getAccounts().stream()
                .map(Account::getNumber)
                .flatMap(number -> entriesByAccount.getOrDefault(number, Collections.emptyList()).stream())
                .collect(Collectors.toList());

        Map<Boolean, BigDecimal> totals = ofMember.stream()
                .collect(Collectors.partitioningBy(Entry::isCredit,
                        Collectors.reducing(BigDecimal.ZERO, Entry::getAmount, BigDecimal::add)));

        return new ReportLine(member.getName(), ofMember.size(), totals.get(true), totals.get(false));
    }

    /** Text for printing at the branch. */
    public String format(List<ReportLine> lines) {
        return lines.stream()
                .map(line -> String.join(";", line.getMemberName(), String.valueOf(line.getCount()),
                        line.getTotalCredit().toPlainString(), line.getTotalDebit().toPlainString()) + "\n")
                .collect(Collectors.joining());
    }
}
