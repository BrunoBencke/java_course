package cerne.transfers;

import java.math.BigDecimal;
import java.util.Date;

import cerne.accounts.Branch;
import cerne.accounts.Account;
import cerne.members.Member;
import cerne.members.MemberType;
import cerne.util.CerneUtil;

/** Test data shared by the transfer tests. */
final class Scenario {

    static final Date BUSINESS_DAY_10AM = CerneUtil.dateTime(2026, 8, 3, 10, 0);
    static final Date BUSINESS_DAY_8PM = CerneUtil.dateTime(2026, 8, 3, 20, 0);
    static final Branch BRANCH = new Branch("0001", "Downtown Branch");

    private Scenario() {
    }

    static Account account(String number, String cpf, MemberType type, String balance) {
        Member holder = new Member(cpf, "Holder " + number, type);
        Account account = new Account(number, BRANCH, holder, new BigDecimal(balance));
        holder.addAccount(account);
        return account;
    }

    static Account individualAccount(String number, String balance) {
        return account(number, "52998224725", MemberType.INDIVIDUAL, balance);
    }
}
