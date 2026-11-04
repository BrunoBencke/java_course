package cerne.transfers.validation;

import cerne.accounts.AccountRepository;
import cerne.transfers.TransferRepository;

/**
 * Rules as data (version C of session 8), with the two challenge rules: AllowedHours and
 * LimitPerMemberType. Each new rule went in here, in the permits, and got a case in RuleEvaluator.
 */
public sealed interface Rule
        permits Rule.PositiveAmount, Rule.ActiveAccount, Rule.ActiveDestination, Rule.AllowedHours,
                Rule.SufficientBalance, Rule.DailyLimit, Rule.LimitPerMemberType {

    record PositiveAmount() implements Rule {
    }

    record ActiveAccount() implements Rule {
    }

    /** Only applies to an internal transfer: the destination must be in Cerne and active. */
    record ActiveDestination(AccountRepository accounts) implements Rule {
    }

    /** Internal: 24h. Between credit unions and to another bank: configurable windows. */
    record AllowedHours(TimeWindow betweenCreditUnions, TimeWindow toOtherBank) implements Rule {

        /** Between credit unions from 6h to 22h; to another bank from 6h30 to 17h, on business days. */
        public static AllowedHours defaults() {
            return new AllowedHours(TimeWindow.BETWEEN_CREDIT_UNIONS, TimeWindow.OTHER_BANK);
        }
    }

    /** Overdraft only counts for a legal entity. */
    record SufficientBalance(boolean considerOverdraft) implements Rule {
    }

    record DailyLimit(TransferRepository transfers) implements Rule {
    }

    /** Only for another bank; the amounts come from the injected policy. */
    record LimitPerMemberType(LimitPolicy policy) implements Rule {
    }
}
