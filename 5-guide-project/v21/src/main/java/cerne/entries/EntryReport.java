package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cerne.accounts.Account;
import cerne.members.Member;

/** Per-member report with Streams: index, line per member, final pipeline. */
public class EntryReport {

    public List<ReportLine> generate(List<Member> members, List<Entry> entries,
                                     LocalDate start, LocalDate end) {
        var entriesByAccount = entries.stream()
                .filter(entry -> entry.isBetween(start, end))
                .collect(Collectors.groupingBy(Entry::account));

        return members.stream()
                .map(member -> lineFor(member, entriesByAccount))
                .filter(line -> line.count() > 0)
                .sorted(Comparator.comparing(ReportLine::totalCredit).reversed())
                .toList();
    }

    private ReportLine lineFor(Member member, Map<String, List<Entry>> entriesByAccount) {
        List<Entry> ofMember = member.getAccounts().stream()
                .map(Account::number)
                .flatMap(number -> entriesByAccount.getOrDefault(number, List.of()).stream())
                .toList();

        Map<Boolean, BigDecimal> totals = ofMember.stream()
                .collect(Collectors.partitioningBy(Entry::isCredit,
                        Collectors.reducing(BigDecimal.ZERO, Entry::amount, BigDecimal::add)));

        return new ReportLine(member.getName(), ofMember.size(), totals.get(true), totals.get(false));
    }

    public String format(List<ReportLine> lines) {
        return lines.stream()
                .map(line -> String.join(";", line.memberName(), String.valueOf(line.count()),
                        line.totalCredit().toPlainString(), line.totalDebit().toPlainString()) + "\n")
                .collect(Collectors.joining());
    }
}
