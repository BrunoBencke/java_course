package cerne.transfers;

import java.math.BigDecimal;
import java.util.Date;

public interface TransferRepository {

    void record(String accountNumber, Date instant, BigDecimal amount);

    BigDecimal dailyTotal(String accountNumber, Date day);
}
