package cerne.members;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cerne.accounts.Account;

/** Member is an entity (identity by CPF): it stays a class, no Lombok, with explicit getters. */
public class Member {

    private final String cpf;
    private String name;
    private MemberType type;
    private final boolean specialAgreement;
    private final List<Account> accounts = new ArrayList<>();
    private Account mainAccount;

    public Member(String cpf, String name, MemberType type) {
        this(cpf, name, type, false);
    }

    public Member(String cpf, String name, MemberType type, boolean specialAgreement) {
        this.cpf = cpf;
        this.name = name;
        this.type = type;
        this.specialAgreement = specialAgreement;
    }

    /** The first account opened becomes the main one. */
    public void addAccount(Account account) {
        accounts.add(account);
        if (mainAccount == null) {
            mainAccount = account;
        }
    }

    public String getCpf() {
        return cpf;
    }

    public String getName() {
        return name;
    }

    public MemberType getType() {
        return type;
    }

    /** A member with a special agreement has no per-transfer limit. */
    public boolean hasSpecialAgreement() {
        return specialAgreement;
    }

    public List<Account> getAccounts() {
        return List.copyOf(accounts);
    }

    public Account getMainAccount() {
        return mainAccount;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Member other && cpf.equals(other.cpf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpf);
    }
}
