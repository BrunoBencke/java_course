package cerne.statements;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import cerne.entries.Entry;

/** Builds the statement in the requested format; each format is a class with a switch. */
public class StatementGenerator {

    public String generate(List<Entry> entries, Function<Entry, String> format) {
        return entries.stream()
                .map(entry -> format.apply(entry) + "\n")
                .collect(Collectors.joining());
    }

    public String generateCsv(List<Entry> entries) {
        return generate(entries, new CsvLineGenerator()::generate);
    }

    public String generateText(List<Entry> entries) {
        return generate(entries, new TextLineGenerator()::generate);
    }
}
