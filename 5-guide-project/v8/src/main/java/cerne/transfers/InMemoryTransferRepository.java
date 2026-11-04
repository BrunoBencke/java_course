package cerne.transfers;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cerne.util.CerneUtil;

public class InMemoryTransferRepository implements TransferRepository {

    // key "account|dd/MM/yyyy", built with CerneUtil's static formatter
    private final Map<String, BigDecimal> totalByAccountAndDay = new HashMap<String, BigDecimal>();

    @Override
    public void record(String accountNumber, Date instant, BigDecimal amount) {
        String key = accountNumber + "|" + CerneUtil.formatDate(instant);
        BigDecimal current = totalByAccountAndDay.get(key);
        totalByAccountAndDay.put(key, current == null ? amount : current.add(amount));
    }

    @Override
    public BigDecimal dailyTotal(String accountNumber, Date day) {
        BigDecimal total = totalByAccountAndDay.get(accountNumber + "|" + CerneUtil.formatDate(day));
        return total == null ? BigDecimal.ZERO : total;
    }
}
