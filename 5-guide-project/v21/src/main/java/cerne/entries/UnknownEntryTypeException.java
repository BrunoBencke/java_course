package cerne.entries;

public class UnknownEntryTypeException extends RuntimeException {

    public UnknownEntryTypeException(String code) {
        super("Unknown entry type: " + code);
    }
}
