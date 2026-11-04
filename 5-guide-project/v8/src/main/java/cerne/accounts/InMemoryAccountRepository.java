package cerne.accounts;

import java.util.HashMap;
import java.util.Map;

public class InMemoryAccountRepository implements AccountRepository {

    private final Map<String, Account> byNumber = new HashMap<String, Account>();

    public void add(Account account) {
        byNumber.put(account.getNumber(), account);
    }

    @Override
    public Account findByNumber(String number) {
        return byNumber.get(number);
    }
}
