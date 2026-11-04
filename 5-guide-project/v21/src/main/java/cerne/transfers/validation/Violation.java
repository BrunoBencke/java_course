package cerne.transfers.validation;

/** A rule that failed: stable code for the log and a message for the member. */
public record Violation(String code, String message) {
}
