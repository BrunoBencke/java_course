package cerne.transfers;

import java.math.BigDecimal;
import java.util.Objects;

import cerne.accounts.Account;

public record InternalTransfer(Account source, Account destination, BigDecimal amount) implements Transfer {

    public InternalTransfer {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        amount = TransferValues.positive(amount);
    }

    @Override
    public String destinationAccountNumber() {
        return destination.number();
    }

    @Override
    public TransferType type() {
        return TransferType.INTERNAL;
    }
}
