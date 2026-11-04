package cerne.transfers;

import java.math.BigDecimal;
import java.util.Objects;

import cerne.accounts.Account;

public record OtherBankTransfer(Account source, ExternalAccount destination, BigDecimal amount)
        implements Transfer {

    public OtherBankTransfer {
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
        return TransferType.TO_OTHER_BANK;
    }
}
