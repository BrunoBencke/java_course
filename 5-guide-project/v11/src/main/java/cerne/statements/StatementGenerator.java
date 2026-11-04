package cerne.statements;

import java.util.List;

import cerne.entries.Entry;

/** Builds the statement in the requested format; the format is a visitor. */
public class StatementGenerator {

    public String generate(List<Entry> entries, EntryVisitor<String> format) {
        StringBuilder statement = new StringBuilder();
        for (Entry entry : entries) {
            statement.append(entry.accept(format)).append('\n');
        }
        return statement.toString();
    }

    public String generateCsv(List<Entry> entries) {
        return generate(entries, new CsvLineGenerator());
    }

    public String generateText(List<Entry> entries) {
        return generate(entries, new TextLineGenerator());
    }
}
