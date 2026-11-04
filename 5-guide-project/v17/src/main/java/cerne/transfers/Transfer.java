package cerne.transfers;

import java.math.BigDecimal;

import cerne.accounts.Account;

/** Closed hierarchy: nobody outside this package creates a fourth transfer type. */
public sealed interface Transfer
        permits InternalTransfer, InterCreditUnionTransfer, OtherBankTransfer {

    Account source();

    BigDecimal amount();

    /** Destination account number, whatever the institution. */
    String destinationAccountNumber();

    TransferType type();
}
