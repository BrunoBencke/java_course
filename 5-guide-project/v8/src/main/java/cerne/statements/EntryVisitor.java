package cerne.statements;

import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.FeeEntry;

/** Classic Visitor: one operation (format) per implementation, one method per type. */
public interface EntryVisitor<R> {

    R visitCredit(CreditEntry credit);

    R visitDebit(DebitEntry debit);

    R visitFee(FeeEntry fee);
}
