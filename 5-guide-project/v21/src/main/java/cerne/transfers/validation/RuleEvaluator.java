package cerne.transfers.validation;

import java.math.BigDecimal;
import java.util.Optional;

import cerne.accounts.Account;
import cerne.members.MemberType;
import cerne.transfers.InterCreditUnionTransfer;
import cerne.transfers.InternalTransfer;
import cerne.transfers.OtherBankTransfer;

/** Centralized dispatch: a switch without default; a rule without a case does not compile. */
public final class RuleEvaluator {

    public Optional<Violation> evaluate(Rule rule, ValidationContext c) {
        return switch (rule) {
            case Rule.PositiveAmount() ->
                    approvedIf(c.request().amount().signum() > 0, "INVALID_AMOUNT", "Amount must be positive");

            case Rule.ActiveAccount() ->
                    approvedIf(c.source().active(), "ACCOUNT_BLOCKED", "Source account blocked");

            case Rule.ActiveDestination(var accounts) -> switch (c.request().transfer()) {
                case InternalTransfer internal -> approvedIf(
                        accounts.findByNumber(internal.destination().number()).map(Account::active).orElse(false),
                        "DESTINATION_BLOCKED", "Destination account blocked or not found");
                case InterCreditUnionTransfer between -> Optional.empty();
                case OtherBankTransfer other -> Optional.empty();
            };

            case Rule.SufficientBalance(var withOverdraft) -> {
                var usesOverdraft = withOverdraft && c.source().holderType() == MemberType.LEGAL_ENTITY;
                var extra = usesOverdraft ? c.source().overdraftLimit().orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
                var available = c.source().balance().add(extra);
                yield approvedIf(available.compareTo(c.request().amount()) >= 0,
                        "INSUFFICIENT_BALANCE", "Insufficient balance");
            }

            case Rule.DailyLimit(var transfers) -> {
                var total = transfers.dailyTotal(c.source().number(), c.now().toLocalDate());
                yield approvedIf(total.add(c.request().amount()).compareTo(c.source().dailyLimit()) <= 0,
                        "DAILY_LIMIT", "Daily limit exceeded");
            }
        };
    }

    private static Optional<Violation> approvedIf(boolean condition, String code, String message) {
        return condition ? Optional.empty() : Optional.of(new Violation(code, message));
    }
}
