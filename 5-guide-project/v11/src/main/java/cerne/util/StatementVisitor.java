package cerne.util;

import cerne.entries.Entry;
import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.FeeEntry;

/** Old "visitor": it is an instanceof cascade, with no coverage guarantee. */
public class StatementVisitor {

    public String visit(Entry e) {
        if (e instanceof CreditEntry) {
            CreditEntry c = (CreditEntry) e;
            return "C " + c.getAmount() + " " + c.getSource();
        } else if (e instanceof DebitEntry) {
            DebitEntry d = (DebitEntry) e;
            return "D " + d.getAmount();
        } else if (e instanceof FeeEntry) {
            FeeEntry f = (FeeEntry) e;
            return "T " + f.getAmount() + " " + f.getFeeCode();
        }
        return "";
    }
}
