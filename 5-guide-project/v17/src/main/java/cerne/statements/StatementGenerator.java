package cerne.statements;

import java.util.List;
import java.util.stream.Collectors;

import cerne.entries.Entry;

/** Builds the statement in the requested format; the format is a visitor. */
public class StatementGenerator {

    public String generate(List<Entry> entries, EntryVisitor<String> format) {
        return entries.stream()
                .map(entry -> entry.accept(format) + "\n")
                .collect(Collectors.joining());
    }

    public String generateCsv(List<Entry> entries) {
        return generate(entries, new CsvLineGenerator());
    }

    public String generateText(List<Entry> entries) {
        return generate(entries, new TextLineGenerator());
    }
}
