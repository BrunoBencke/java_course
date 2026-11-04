package cerne.transfers.validation;

import cerne.accounts.AccountRepository;
import cerne.transfers.TransferRepository;

/**
 * Rules as data (version C of session 8): each rule is a record; evaluation is centralized in the
 * exhaustive switch of RuleEvaluator. New rule = a record here, in the permits, and a case there.
 */
public sealed interface Rule
        permits Rule.PositiveAmount, Rule.ActiveAccount, Rule.ActiveDestination, Rule.SufficientBalance, Rule.DailyLimit {

    record PositiveAmount() implements Rule {
    }

    record ActiveAccount() implements Rule {
    }

    /** Only applies to an internal transfer: the destination must be in Cerne and active. */
    record ActiveDestination(AccountRepository accounts) implements Rule {
    }

    /** Overdraft only counts for a legal entity. */
    record SufficientBalance(boolean considerOverdraft) implements Rule {
    }

    record DailyLimit(TransferRepository transfers) implements Rule {
    }
}
