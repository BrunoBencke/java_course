package cerne.transfers;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

import cerne.audit.AuditLog;
import cerne.audit.AuditedTransferService;

/**
 * The order of the decorators is a business decision, written in a single place (from outside in):
 * audit (sees every attempt, including the blocked ones) → deduplication (a resend returns the receipt without
 * re-evaluating the limit) → daily limit (blocks before debiting) → default service.
 */
public final class TransferServiceComposition {

    private TransferServiceComposition() {
    }

    public static TransferService compose(TransferService defaultService, AuditLog audit,
                                          TransferRepository transfers, Clock clock) {
        return new AuditedTransferService(
                new DeduplicatingTransferService(
                        new DailyLimitTransferService(defaultService, transfers, clock),
                        new ConcurrentHashMap<>()),
                audit);
    }
}
