package cerne.accounts;

public interface AccountRepository {

    /** Returns the account, or null when the number does not exist. */
    Account findByNumber(String number);
}
