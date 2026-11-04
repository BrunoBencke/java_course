package cerne.audit;

import java.math.BigDecimal;

/** Event written to the trail. With Jackson, it can be a record. */
public record AuditEvent(String type, String key, String account, BigDecimal amount, String detail) {
}
