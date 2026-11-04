package cerne.measurement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import cerne.accounts.Branch;
import cerne.accounts.Account;
import cerne.members.Member;
import cerne.members.MemberType;
import cerne.entries.Entry;
import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.FeeEntry;
import cerne.transfers.TransferRecord;

/** Synthetic dataset with a fixed seed: the same run always generates the same data. */
public final class DatasetGenerator {

    private DatasetGenerator() {
    }

    /** Members with one or two accounts each (1.5 on average). */
    public static List<Member> members(int count) {
        Random random = new Random(42);
        Branch branch = new Branch("0001", "Downtown Branch");
        MemberType[] types = MemberType.values();
        List<Member> members = new ArrayList<Member>(count);
        for (int i = 0; i < count; i++) {
            Member member = new Member(String.format("%011d", i + 1), "Member " + (i + 1),
                    types[i % types.length]);
            int accounts = random.nextBoolean() ? 1 : 2;
            for (int k = 0; k < accounts; k++) {
                member.addAccount(new Account(String.format("%06d-%d", i + 1, k), branch, member,
                        new BigDecimal("1000.00")));
            }
            members.add(member);
        }
        return members;
    }

    /** Entries between 25/07/2026 and 07/09/2026: some fall outside August. */
    public static List<Entry> entries(List<Member> members, int count) {
        Random random = new Random(7);
        List<Account> accounts = new ArrayList<>();
        for (Member member : members) {
            accounts.addAll(member.getAccounts());
        }
        LocalDate firstDay = LocalDate.of(2026, 7, 25);
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String account = accounts.get(random.nextInt(accounts.size())).getNumber();
            LocalDate date = firstDay.plusDays(random.nextInt(45));
            BigDecimal amount = BigDecimal.valueOf(100 + random.nextInt(100_000), 2);
            int draw = random.nextInt(10);
            if (draw < 5) {
                entries.add(new CreditEntry(account, amount, date));
            } else if (draw < 9) {
                entries.add(new DebitEntry(account, amount, date));
            } else {
                entries.add(new FeeEntry(account, amount, date));
            }
        }
        return entries;
    }

    /**
     * Two sides of the reconciliation: ~90% match, ~4% only in Cerne, ~4% only in the file and ~2% with a
     * mismatched amount. The file arrives shuffled.
     */
    public static ReconciliationDataset reconciliation(int count) {
        Random random = new Random(11);
        LocalDate start = LocalDate.of(2026, 8, 1);
        List<TransferRecord> fromCerne = new ArrayList<TransferRecord>(count);
        List<TransferRecord> fromFile = new ArrayList<TransferRecord>(count);
        for (int i = 0; i < count; i++) {
            String identifier = "TRF-" + String.format("%08d", i + 1);
            BigDecimal amount = BigDecimal.valueOf(100 + random.nextInt(500_000), 2);
            LocalDate date = start.plusDays(random.nextInt(28));
            int draw = random.nextInt(100);
            TransferRecord fromSystem = new TransferRecord(identifier, amount, date);
            if (draw < 90) {
                fromCerne.add(fromSystem);
                fromFile.add(new TransferRecord(identifier.toLowerCase() + " ", amount, date));
            } else if (draw < 94) {
                fromCerne.add(fromSystem);
            } else if (draw < 98) {
                fromFile.add(fromSystem);
            } else {
                fromCerne.add(fromSystem);
                fromFile.add(new TransferRecord(identifier, amount.add(BigDecimal.ONE), date));
            }
        }
        Collections.shuffle(fromFile, new Random(13));
        return new ReconciliationDataset(fromCerne, fromFile);
    }

    /** Lines "account;date;type;amount;description" for EntryProcessing (identical across milestones). */
    public static List<String> entryLines(int count, int accountCount) {
        Random random = new Random(2026);
        List<String> lines = new ArrayList<String>(count);
        StringBuilder line = new StringBuilder(96);
        for (int i = 0; i < count; i++) {
            int account = random.nextInt(accountCount);
            int day = 1 + random.nextInt(28);
            int draw = random.nextInt(10);
            int cents = 100 + random.nextInt(500_000);
            line.setLength(0);
            line.append("ACCOUNT-").append(account).append(';')
                    .append("2026-08-").append(day < 10 ? "0" : "").append(day).append(';')
                    .append(draw < 5 ? 'C' : draw < 9 ? 'D' : 'T').append(';')
                    .append(cents / 100).append('.').append(cents % 100 < 10 ? "0" : "").append(cents % 100)
                    .append(';').append("Synthetic entry number ").append(i);
            lines.add(line.toString());
        }
        return lines;
    }
}
