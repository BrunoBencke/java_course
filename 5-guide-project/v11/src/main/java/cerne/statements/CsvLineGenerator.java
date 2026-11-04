package cerne.statements;

import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.FeeEntry;
import cerne.util.CerneUtil;

/** CSV line for the member's accountant: date;type;amount;complement. */
public class CsvLineGenerator implements EntryVisitor<String> {

    @Override
    public String visitCredit(CreditEntry c) {
        return String.join(";", CerneUtil.formatDate(c.getDate()), "CREDIT", c.getAmount().toPlainString(),
                c.getSource() == null ? "" : c.getSource());
    }

    @Override
    public String visitDebit(DebitEntry d) {
        return String.join(";", CerneUtil.formatDate(d.getDate()), "DEBIT", d.getAmount().negate().toPlainString(), "");
    }

    @Override
    public String visitFee(FeeEntry f) {
        return String.join(";", CerneUtil.formatDate(f.getDate()), "FEE", f.getAmount().negate().toPlainString(),
                f.getFeeCode() == null ? "" : f.getFeeCode());
    }
}
