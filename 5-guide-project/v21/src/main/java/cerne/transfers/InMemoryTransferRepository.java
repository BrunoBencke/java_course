package cerne.transfers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class InMemoryTransferRepository implements TransferRepository {

    private record AccountOnDay(String accountNumber, LocalDate day) {
    }

    private final Map<AccountOnDay, BigDecimal> totals = new HashMap<>();

    @Override
    public void record(String accountNumber, LocalDate day, BigDecimal amount) {
        totals.merge(new AccountOnDay(accountNumber, day), amount, BigDecimal::add);
    }

    @Override
    public BigDecimal dailyTotal(String accountNumber, LocalDate day) {
        return totals.getOrDefault(new AccountOnDay(accountNumber, day), BigDecimal.ZERO);
    }
}
