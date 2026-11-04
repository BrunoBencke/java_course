package cerne.transfers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import cerne.accounts.Account;
import cerne.accounts.Branch;
import cerne.accounts.InMemoryAccountRepository;
import cerne.members.Member;
import cerne.members.MemberType;

/** Test data shared by the transfer tests. */
final class Scenario {

    static final ZoneId TIME_ZONE = ZoneId.of("America/Sao_Paulo");
    static final Clock MONDAY_10AM = clock(LocalDateTime.of(2026, 8, 3, 10, 0));
    static final Clock MONDAY_8PM = clock(LocalDateTime.of(2026, 8, 3, 20, 0));
    static final Branch BRANCH = new Branch("0001", "Downtown Branch");
    static final ExternalAccount EXTERNAL_ACCOUNT = new ExternalAccount("00000208", "1234", "99887-6");

    private Scenario() {
    }

    static Clock clock(LocalDateTime instant) {
        return Clock.fixed(instant.atZone(TIME_ZONE).toInstant(), TIME_ZONE);
    }

    static Account account(String number, Member holder, String balance) {
        var account = new Account(number, BRANCH, holder, new BigDecimal(balance));
        holder.addAccount(account);
        return account;
    }

    static Account individualAccount(String number, String balance) {
        return account(number, new Member("52998224725", "Ana", MemberType.INDIVIDUAL), balance);
    }

    static Account legalEntityAccount(String number, String balance) {
        return account(number, new Member("11144477735", "Agro Cooperative Ltd", MemberType.LEGAL_ENTITY), balance);
    }

    /** Member with a special agreement: no per-transfer limit to another bank. */
    static Account accountWithAgreement(String number, String balance) {
        return account(number, new Member("12345678909", "Bruno", MemberType.INDIVIDUAL, true), balance);
    }

    static InMemoryAccountRepository repository(Account... accounts) {
        var repository = new InMemoryAccountRepository();
        for (Account account : accounts) {
            repository.add(account);
        }
        return repository;
    }

    static TransferRequest request(Transfer transfer) {
        return new TransferRequest(transfer, new IdempotencyKey("request-1"));
    }
}
