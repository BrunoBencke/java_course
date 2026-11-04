package cerne.statements;

import cerne.entries.Credit;
import cerne.entries.Debit;
import cerne.entries.Fee;
import cerne.entries.Reversal;

/** Classic Visitor: on 17 it is still the way to get per-type dispatch with guaranteed coverage. */
public interface EntryVisitor<R> {

    R visitCredit(Credit credit);

    R visitDebit(Debit debit);

    R visitFee(Fee fee);

    R visitReversal(Reversal reversal);
}
