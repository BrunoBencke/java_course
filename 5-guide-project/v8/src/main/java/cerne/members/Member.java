package cerne.members;

import java.util.ArrayList;
import java.util.List;

import cerne.accounts.Account;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** Member: identity by CPF (it is an entity, not a value). */
@Getter
@Setter
@EqualsAndHashCode(of = "cpf")
public class Member {

    private String cpf;
    private String name;
    private MemberType type;
    private List<Account> accounts = new ArrayList<Account>();
    private Account mainAccount;

    public Member() {
    }

    public Member(String cpf, String name, MemberType type) {
        this.cpf = cpf;
        this.name = name;
        this.type = type;
    }

    /** The first account opened becomes the main one. */
    public void addAccount(Account account) {
        accounts.add(account);
        if (mainAccount == null) {
            mainAccount = account;
        }
    }
}
