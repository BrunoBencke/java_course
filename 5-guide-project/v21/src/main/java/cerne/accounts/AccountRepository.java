package cerne.accounts;

import java.util.Optional;

/** A single abstract method: a lambda works as a fake repository in the tests. */
@FunctionalInterface
public interface AccountRepository {

    Optional<Account> findByNumber(String number);
}
