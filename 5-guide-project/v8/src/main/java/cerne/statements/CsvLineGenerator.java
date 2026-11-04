package cerne.statements;

import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.FeeEntry;
import cerne.util.CerneUtil;

/** CSV line for the member's accountant: date;type;amount;complement. */
public class CsvLineGenerator implements EntryVisitor<String> {

    @Override
    public String visitCredit(CreditEntry c) {
        return line(CerneUtil.formatDate(c.getDate()), "CREDIT", String.valueOf(c.getAmount()),
                c.getSource() == null ? "" : c.getSource());
    }

    @Override
    public String visitDebit(DebitEntry d) {
        return line(CerneUtil.formatDate(d.getDate()), "DEBIT", String.valueOf(-d.getAmount()), "");
    }

    @Override
    public String visitFee(FeeEntry t) {
        return line(CerneUtil.formatDate(t.getDate()), "FEE", String.valueOf(-t.getAmount()),
                t.getFeeCode() == null ? "" : t.getFeeCode());
    }

    private String line(String date, String type, String amount, String complement) {
        return date + ";" + type + ";" + amount + ";" + complement;
    }
}
