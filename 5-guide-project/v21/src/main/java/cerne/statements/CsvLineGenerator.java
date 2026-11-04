package cerne.statements;

import java.time.format.DateTimeFormatter;

import cerne.entries.Credit;
import cerne.entries.Debit;
import cerne.entries.Entry;
import cerne.entries.Fee;
import cerne.entries.Reversal;

/**
 * CSV line: date;type;amount;complement. In 17 it was a Visitor; here it is an exhaustive switch with record
 * patterns, no default: a fifth entry type makes this class stop compiling.
 */
public final class CsvLineGenerator {

    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String generate(Entry entry) {
        return switch (entry) {
            case Credit(var account, var amount, var date, var source) ->
                    String.join(";", DAY_MONTH_YEAR.format(date), "CREDIT", amount.toPlainString(), source);
            case Debit(var account, var amount, var date) ->
                    String.join(";", DAY_MONTH_YEAR.format(date), "DEBIT", amount.negate().toPlainString(), "");
            case Fee(var account, var amount, var date, var code) ->
                    String.join(";", DAY_MONTH_YEAR.format(date), "FEE", amount.negate().toPlainString(), code);
            case Reversal(var account, var amount, var date, var original) ->
                    String.join(";", DAY_MONTH_YEAR.format(date), "REVERSAL", amount.toPlainString(), original.toString());
        };
    }
}
