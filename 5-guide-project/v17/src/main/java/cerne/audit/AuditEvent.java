package cerne.audit;

import java.math.BigDecimal;

/** Event written to the trail (plain class: XStream 1.4.9 does not know records). */
public class AuditEvent {

    private final String type;
    private final String key;
    private final String account;
    private final BigDecimal amount;
    private final String detail;

    public AuditEvent(String type, String key, String account, BigDecimal amount, String detail) {
        this.type = type;
        this.key = key;
        this.account = account;
        this.amount = amount;
        this.detail = detail;
    }

    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getAccount() {
        return account;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDetail() {
        return detail;
    }
}
