package cerne.util;

import java.util.Date;

import cerne.transfers.TransferService;
import cerne.transfers.Transfer;

/** Old "decorator": it is inheritance, not composition (you cannot stack two). */
public class ServiceDecorator extends TransferService {

    @Override
    public String transfer(Transfer transfer, Date instant) {
        CerneUtil.log("Starting transfer from " + transfer.getSource().getNumber());
        String identifier = super.transfer(transfer, instant);
        CerneUtil.log("Transfer " + identifier + " completed");
        return identifier;
    }
}
