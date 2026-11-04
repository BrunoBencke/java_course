package cerne.transfers.validation;

import java.util.List;

public sealed interface ValidationResult {

    record Approved() implements ValidationResult {
    }

    record Rejected(List<Violation> violations) implements ValidationResult {

        public Rejected {
            violations = List.copyOf(violations);
        }
    }
}
