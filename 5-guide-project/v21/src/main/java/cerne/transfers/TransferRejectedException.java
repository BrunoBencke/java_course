package cerne.transfers;

import java.util.List;
import java.util.stream.Collectors;

import cerne.transfers.validation.Violation;

/** All violations at once (a customer service request since 2019). */
public class TransferRejectedException extends RuntimeException {

    private final List<Violation> violations;

    public TransferRejectedException(List<Violation> violations) {
        super(violations.stream().map(Violation::code).collect(Collectors.joining(", ")));
        this.violations = List.copyOf(violations);
    }

    public List<Violation> violations() {
        return violations;
    }
}
