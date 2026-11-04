package cerne.transfers;

import java.util.UUID;

/** Identifies a client request: the same request sent again has the same key. */
public record IdempotencyKey(String value) {

    public IdempotencyKey {
        value = TransferValues.notBlank(value, "idempotency key");
    }

    public static IdempotencyKey newKey() {
        return new IdempotencyKey(UUID.randomUUID().toString());
    }
}
