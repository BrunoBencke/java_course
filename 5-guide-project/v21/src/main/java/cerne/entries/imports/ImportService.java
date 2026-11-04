package cerne.entries.imports;

import java.util.List;

import cerne.entries.Entry;
import cerne.entries.EntryFactory;

/** Imports the daily entries file; creation now lives entirely in the factory. */
public class ImportService {

    public List<Entry> importLines(List<FileLine> lines) {
        return lines.stream().map(EntryFactory::from).toList();
    }
}
