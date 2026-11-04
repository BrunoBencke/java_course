package cerne.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cerne.accounts.Account;
import cerne.accounts.Branch;
import cerne.members.Member;
import cerne.members.MemberType;
import cerne.transfers.IdempotencyKey;
import cerne.transfers.InternalTransfer;
import cerne.transfers.TransferRequest;

/** Exercises XStream 1.4.9: without the pom's --add-opens, it fails with InaccessibleObjectException. */
class AuditServiceTest {

    @Test
    @DisplayName("writes the attempt as XML")
    void writesAttemptAsXml() {
        var holder = new Member("52998224725", "Ana", MemberType.INDIVIDUAL);
        var branch = new Branch("0001", "Downtown");
        var request = new TransferRequest(new InternalTransfer(
                new Account("0001-1", branch, holder, BigDecimal.TEN), new Account("0002-2", branch, holder, BigDecimal.ZERO),
                BigDecimal.ONE), new IdempotencyKey("request-7"));

        var audit = new AuditService();
        audit.recordAttempt(request);

        assertEquals(1, audit.records().size());
        assertTrue(audit.records().get(0).contains("<key>request-7</key>"));
    }
}
