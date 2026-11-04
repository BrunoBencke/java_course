package cerne.statements;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

import cerne.entries.Credit;
import cerne.entries.Debit;
import cerne.entries.Fee;
import cerne.entries.Reversal;

/** Text line for printing at the branch (Visitor, v17 version). */
public class TextLineGenerator implements EntryVisitor<String> {

    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String visitCredit(Credit c) {
        return DAY_MONTH_YEAR.format(c.date()) + "  CREDIT   +" + inReais(c.amount());
    }

    @Override
    public String visitDebit(Debit d) {
        return DAY_MONTH_YEAR.format(d.date()) + "  DEBIT    -" + inReais(d.amount());
    }

    @Override
    public String visitFee(Fee f) {
        return DAY_MONTH_YEAR.format(f.date()) + "  FEE      -" + inReais(f.amount());
    }

    @Override
    public String visitReversal(Reversal r) {
        return DAY_MONTH_YEAR.format(r.date()) + "  REVERSAL +" + inReais(r.amount());
    }

    private static String inReais(BigDecimal amount) {
        return "R$ " + amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString().replace('.', ',');
    }
}
