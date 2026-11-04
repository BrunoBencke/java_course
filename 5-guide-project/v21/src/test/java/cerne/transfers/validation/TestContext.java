package cerne.transfers.validation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import cerne.accounts.Account;
import cerne.accounts.Branch;
import cerne.members.Member;
import cerne.members.MemberType;
import cerne.transfers.ExternalAccount;
import cerne.transfers.InternalTransfer;
import cerne.transfers.OtherBankTransfer;
import cerne.transfers.Transfer;
import cerne.transfers.TransferRequest;

/** Ready-made contexts for testing rules in isolation. */
final class TestContext {

    static final LocalDateTime MONDAY_10AM = LocalDateTime.of(2026, 8, 3, 10, 0);
    static final Branch BRANCH = new Branch("0001", "Downtown");

    private TestContext() {
    }

    static Account account(String number, MemberType type, String balance) {
        var holder = new Member("52998224725", "Holder " + number, type);
        var account = new Account(number, BRANCH, holder, new BigDecimal(balance));
        holder.addAccount(account);
        return account;
    }

    static ValidationContext of(Transfer transfer) {
        return new ValidationContext(TransferRequest.of(transfer), transfer.source(), MONDAY_10AM);
    }

    static ValidationContext withDestination(String destinationNumber) {
        return of(new InternalTransfer(account("0001-1", MemberType.INDIVIDUAL, "1000.00"),
                account(destinationNumber, MemberType.INDIVIDUAL, "0.00"), BigDecimal.TEN));
    }

    static ValidationContext toOtherBank(Account source, String amount) {
        return of(new OtherBankTransfer(source, new ExternalAccount("00000208", "1234", "99887-6"),
                new BigDecimal(amount)));
    }
}
