package cerne.entries;

public enum EntryType {
    CREDIT,
    DEBIT,
    FEE,
    REVERSAL;

    /** Credit and reversal come into the account; debit and fee go out. */
    public boolean isInflow() {
        return this == CREDIT || this == REVERSAL;
    }
}
