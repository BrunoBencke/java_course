package cerne.statements;

import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.FeeEntry;
import cerne.util.CerneUtil;

/** Text line for printing at the branch. */
public class TextLineGenerator implements EntryVisitor<String> {

    @Override
    public String visitCredit(CreditEntry c) {
        return CerneUtil.formatDate(c.getDate()) + "  CREDIT   +" + CerneUtil.formatAmount(c.getAmount());
    }

    @Override
    public String visitDebit(DebitEntry d) {
        return CerneUtil.formatDate(d.getDate()) + "  DEBIT    -" + CerneUtil.formatAmount(d.getAmount());
    }

    @Override
    public String visitFee(FeeEntry f) {
        return CerneUtil.formatDate(f.getDate()) + "  FEE      -" + CerneUtil.formatAmount(f.getAmount());
    }
}
