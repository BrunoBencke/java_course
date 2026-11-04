package cerne.entries;

import cerne.entries.imports.FileLine;

/**
 * Factory (session 7): the single point that translates the file code into the concrete type.
 * Callers depend only on Entry. The default exists because the selector is a String, not the sealed type.
 */
public final class EntryFactory {

    private EntryFactory() {
    }

    public static Entry from(FileLine line) {
        return switch (line.type()) {
            case "C" -> new Credit(line.account(), line.amount(), line.date(), line.source());
            case "D" -> new Debit(line.account(), line.amount(), line.date());
            case "T" -> new Fee(line.account(), line.amount(), line.date(), line.feeCode());
            case "E" -> new Reversal(line.account(), line.amount(), line.date(), line.originalEntry());
            default -> throw new UnknownEntryTypeException(line.type());
        };
    }
}
