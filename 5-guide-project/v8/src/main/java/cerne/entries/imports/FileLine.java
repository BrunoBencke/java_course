package cerne.entries.imports;

import java.util.Date;

/** One line of the entries file received from the central. */
public class FileLine {

    private String type;
    private String account;
    private double amount;
    private Date date;
    private String source;
    private String feeCode;

    public FileLine(String type, String account, double amount, Date date) {
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

    public double getAmount() {
        return amount;
    }

    public Date getDate() {
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
