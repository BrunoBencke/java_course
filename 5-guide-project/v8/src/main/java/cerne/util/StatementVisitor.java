package cerne.util;

import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.Entry;
import cerne.entries.FeeEntry;

/** Old "Visitor": it is a cascade of instanceof, with no coverage guarantee. */
public class StatementVisitor {

    public String visit(Entry l) {
        if (l instanceof CreditEntry) {
            CreditEntry c = (CreditEntry) l;
            return "C " + c.getAmount() + " " + c.getSource();
        } else if (l instanceof DebitEntry) {
            DebitEntry d = (DebitEntry) l;
            return "D " + d.getAmount();
        } else if (l instanceof FeeEntry) {
            FeeEntry t = (FeeEntry) l;
            return "T " + t.getAmount() + " " + t.getFeeCode();
        }
        return "";
    }
}
