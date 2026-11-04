package cerne.util;

import cerne.entries.CreditEntry;
import cerne.entries.DebitEntry;
import cerne.entries.FeeEntry;

/** Old "Factory": returns Object and forces a cast; nobody uses it. */
public class EntryFactory {

    public Object create(String type) {
        if ("C".equals(type)) {
            return new CreditEntry();
        } else if ("D".equals(type)) {
            return new DebitEntry();
        } else if ("T".equals(type)) {
            return new FeeEntry();
        }
        return null;
    }
}
