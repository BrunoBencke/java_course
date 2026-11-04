package cerne.audit;

import java.math.BigDecimal;

/** Event written to the audit trail (serialized by XStream). */
public class AuditEvent {

    private String type;
    private String identifier;
    private String account;
    private BigDecimal amount;
    private String instant;

    public AuditEvent() {
    }

    public AuditEvent(String type, String identifier, String account, BigDecimal amount, String instant) {
        this.type = type;
        this.identifier = identifier;
        this.account = account;
        this.amount = amount;
        this.instant = instant;
    }

    public String getType() {
        return type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getAccount() {
        return account;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getInstant() {
        return instant;
    }
}
