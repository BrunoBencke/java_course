package cerne.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

/** Jackson instead of XStream: no --add-opens in the pom. */
class AuditServiceTest {

    @Test
    @DisplayName("writes the attempt as JSON from a record")
    void writesAttemptAsJson() {
        var holder = new Member("52998224725", "Ana", MemberType.INDIVIDUAL);
        var branch = new Branch("0001", "Downtown");
        var request = new TransferRequest(new InternalTransfer(
                new Account("0001-1", branch, holder, BigDecimal.TEN), new Account("0002-2", branch, holder, BigDecimal.ZERO),
                BigDecimal.ONE), new IdempotencyKey("request-7"));

        var audit = new AuditService();
        audit.recordAttempt(request);

        assertEquals("""
                {"type":"ATTEMPT","key":"request-7","account":"0001-1","amount":1.00,"detail":""}""",
                audit.records().get(0));
    }
}
