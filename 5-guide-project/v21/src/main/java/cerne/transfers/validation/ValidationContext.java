package cerne.transfers.validation;

import java.time.LocalDateTime;

import cerne.accounts.Account;
import cerne.transfers.TransferRequest;

/** The request already enriched with the source account and the instant (from the Clock injected in the service). */
public record ValidationContext(TransferRequest request, Account source, LocalDateTime now) {
}
