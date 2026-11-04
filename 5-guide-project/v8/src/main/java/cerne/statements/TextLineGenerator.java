package cerne.statements;

import java.math.BigDecimal;

import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.FeeEntry;
import cerne.util.CerneUtil;

/** Text line for printing at the branch. */
public class TextLineGenerator implements EntryVisitor<String> {

    @Override
    public String visitCredit(CreditEntry c) {
        return CerneUtil.formatDate(c.getDate()) + "  CREDIT   +" + CerneUtil.formatAmount(BigDecimal.valueOf(c.getAmount()));
    }

    @Override
    public String visitDebit(DebitEntry d) {
        return CerneUtil.formatDate(d.getDate()) + "  DEBIT    -" + CerneUtil.formatAmount(BigDecimal.valueOf(d.getAmount()));
    }

    @Override
    public String visitFee(FeeEntry t) {
        return CerneUtil.formatDate(t.getDate()) + "  FEE      -" + CerneUtil.formatAmount(BigDecimal.valueOf(t.getAmount()));
    }
}
