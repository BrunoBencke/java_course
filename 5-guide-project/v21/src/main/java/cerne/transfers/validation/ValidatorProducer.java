package cerne.transfers.validation;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import cerne.accounts.AccountRepository;
import cerne.transfers.TransferRepository;

/** The order of the chain is written in a single place. */
@ApplicationScoped
public class ValidatorProducer {

    @Inject
    TransferRepository transfers;

    @Inject
    AccountRepository accounts;

    @Produces
    @ApplicationScoped
    TransferValidator validator() {
        return new TransferValidator(defaultRules(accounts, transfers));
    }

    /** Cheap and I/O-free first; whatever queries a repository last. */
    public static List<Rule> defaultRules(AccountRepository accounts, TransferRepository transfers) {
        return List.of(
                new Rule.PositiveAmount(),
                new Rule.ActiveAccount(),
                new Rule.ActiveDestination(accounts),
                new Rule.SufficientBalance(true),
                new Rule.DailyLimit(transfers));
    }
}
