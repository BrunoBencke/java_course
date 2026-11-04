package cerne.statements;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

import cerne.entries.Credit;
import cerne.entries.Debit;
import cerne.entries.Entry;
import cerne.entries.Fee;
import cerne.entries.Reversal;

/** Text line for printing at the branch. Each case uses one component: a simple type pattern is enough. */
public final class TextLineGenerator {

    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String generate(Entry entry) {
        var signAndType = switch (entry) {
            case Credit credit -> "CREDIT   +";
            case Debit debit -> "DEBIT    -";
            case Fee fee -> "FEE      -";
            case Reversal reversal -> "REVERSAL +";
        };
        return DAY_MONTH_YEAR.format(entry.date()) + "  " + signAndType + inReais(entry.amount());
    }

    private static String inReais(BigDecimal amount) {
        return "R$ " + amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString().replace('.', ',');
    }
}
