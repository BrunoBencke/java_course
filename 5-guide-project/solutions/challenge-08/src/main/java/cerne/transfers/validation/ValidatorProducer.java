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

    @Inject
    LimitPolicy policy;

    @Produces
    @ApplicationScoped
    TransferValidator validator() {
        return new TransferValidator(defaultRules(accounts, transfers, policy));
    }

    /** Session 8 chain, without the challenge rules (kept for the existing tests). */
    public static List<Rule> defaultRules(AccountRepository accounts, TransferRepository transfers) {
        return List.of(
                new Rule.PositiveAmount(),
                new Rule.ActiveAccount(),
                new Rule.ActiveDestination(accounts),
                new Rule.SufficientBalance(true),
                new Rule.DailyLimit(transfers));
    }

    /**
     * Chain with the challenge rules. Hours come before balance: they depend neither on the amount nor on a
     * repository query, and a member outside the window does not need to hear about the balance. Limit per type
     * comes last: it only makes sense for whoever already passed balance and daily limit.
     */
    public static List<Rule> defaultRules(AccountRepository accounts, TransferRepository transfers,
                                          LimitPolicy policy) {
        return List.of(
                new Rule.PositiveAmount(),
                new Rule.ActiveAccount(),
                new Rule.ActiveDestination(accounts),
                Rule.AllowedHours.defaults(),
                new Rule.SufficientBalance(true),
                new Rule.DailyLimit(transfers),
                new Rule.LimitPerMemberType(policy));
    }
}
