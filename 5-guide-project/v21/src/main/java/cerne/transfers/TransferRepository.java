package cerne.transfers;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TransferRepository {

    void record(String accountNumber, LocalDate day, BigDecimal amount);

    BigDecimal dailyTotal(String accountNumber, LocalDate day);
}
