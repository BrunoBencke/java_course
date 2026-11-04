package cerne.transfers;

import java.math.BigDecimal;
import java.util.Objects;

/** The request that reaches the service: the transfer and the key that detects resends. */
public record TransferRequest(Transfer transfer, IdempotencyKey idempotencyKey) {

    public TransferRequest {
        Objects.requireNonNull(transfer, "transfer");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    }

    public static TransferRequest of(Transfer transfer) {
        return new TransferRequest(transfer, IdempotencyKey.newKey());
    }

    public BigDecimal amount() {
        return transfer.amount();
    }

    public String sourceAccount() {
        return transfer.source().number();
    }

    public String destinationAccount() {
        return transfer.destinationAccountNumber();
    }
}
