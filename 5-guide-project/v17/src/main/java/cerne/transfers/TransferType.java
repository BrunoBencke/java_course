package cerne.transfers;

/** Discriminator of the sealed hierarchy: on 17 it is what the switch expression can enumerate. Gone in 21. */
public enum TransferType {
    INTERNAL,
    BETWEEN_CREDIT_UNIONS,
    TO_OTHER_BANK
}
