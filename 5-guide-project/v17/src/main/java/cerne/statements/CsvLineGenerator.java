package cerne.statements;

import java.time.format.DateTimeFormatter;

import cerne.entries.Credit;
import cerne.entries.Debit;
import cerne.entries.Fee;
import cerne.entries.Reversal;

/** CSV line for the member's accountant: date;type;amount;complement (Visitor, v17 version). */
public class CsvLineGenerator implements EntryVisitor<String> {

    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String visitCredit(Credit c) {
        return String.join(";", DAY_MONTH_YEAR.format(c.date()), "CREDIT", c.amount().toPlainString(), c.source());
    }

    @Override
    public String visitDebit(Debit d) {
        return String.join(";", DAY_MONTH_YEAR.format(d.date()), "DEBIT", d.amount().negate().toPlainString(), "");
    }

    @Override
    public String visitFee(Fee f) {
        return String.join(";", DAY_MONTH_YEAR.format(f.date()), "FEE", f.amount().negate().toPlainString(),
                f.feeCode());
    }

    @Override
    public String visitReversal(Reversal r) {
        return String.join(";", DAY_MONTH_YEAR.format(r.date()), "REVERSAL", r.amount().toPlainString(),
                r.originalEntry().toString());
    }
}
