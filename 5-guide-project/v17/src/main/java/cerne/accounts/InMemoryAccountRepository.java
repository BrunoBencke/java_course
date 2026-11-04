package cerne.accounts;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryAccountRepository implements AccountRepository {

    private final Map<String, Account> byNumber = new HashMap<>();

    public void add(Account account) {
        byNumber.put(account.number(), account);
    }

    @Override
    public Optional<Account> findByNumber(String number) {
        return Optional.ofNullable(byNumber.get(number));
    }
}
