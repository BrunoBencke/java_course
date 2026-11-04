package cerne.entries.imports;

import java.util.ArrayList;
import java.util.List;

import cerne.entries.Entry;
import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.FeeEntry;

/** Imports the daily entries file. The construction rule is repeated in other services. */
public class ImportService {

    public List<Entry> importLines(List<FileLine> lines) {
        List<Entry> entries = new ArrayList<Entry>();
        for (FileLine line : lines) {
            entries.add(buildEntry(line));
        }
        return entries;
    }

    public Entry buildEntry(FileLine line) {
        Entry entry;
        if ("C".equals(line.getType())) {
            entry = new CreditEntry(line.getAccount(), line.getAmount(), line.getDate());
            ((CreditEntry) entry).setSource(line.getSource());
        } else if ("D".equals(line.getType())) {
            entry = new DebitEntry(line.getAccount(), line.getAmount(), line.getDate());
        } else if ("T".equals(line.getType())) {
            entry = new FeeEntry(line.getAccount(), line.getAmount(), line.getDate());
            ((FeeEntry) entry).setFeeCode(line.getFeeCode());
        } else {
            throw new IllegalArgumentException("Unknown type: " + line.getType());
        }
        return entry;
    }
}
