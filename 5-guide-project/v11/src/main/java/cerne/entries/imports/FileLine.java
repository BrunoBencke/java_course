package cerne.entries.imports;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One line of the entries file received from the central (type as code "C", "D", "T"). */
public class FileLine {

    private final String type;
    private final String account;
    private final BigDecimal amount;
    private final LocalDate date;
    private String source;
    private String feeCode;

    public FileLine(String type, String account, BigDecimal amount, LocalDate date) {
        this.type = type;
        this.account = account;
        this.amount = amount;
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public String getAccount() {
        return account;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFeeCode() {
        return feeCode;
    }

    public void setFeeCode(String feeCode) {
        this.feeCode = feeCode;
    }
}
