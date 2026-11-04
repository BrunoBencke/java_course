package cerne.transfers.validation;

import java.util.List;
import java.util.Optional;

/**
 * Chain of Responsibility as a list: every rule is evaluated, in list order, and all
 * violations come back together. Not final: CDI needs a proxy for @ApplicationScoped.
 */
public class TransferValidator {

    private final List<Rule> rules;
    private final RuleEvaluator evaluator = new RuleEvaluator();

    public TransferValidator(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public ValidationResult validate(ValidationContext context) {
        var violations = rules.stream()
                .map(rule -> evaluator.evaluate(rule, context))
                .flatMap(Optional::stream)
                .toList();
        return violations.isEmpty()
                ? new ValidationResult.Approved()
                : new ValidationResult.Rejected(violations);
    }

    public List<Rule> rules() {
        return rules;
    }
}
