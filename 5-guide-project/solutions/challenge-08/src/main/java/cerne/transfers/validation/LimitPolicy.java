package cerne.transfers.validation;

import java.math.BigDecimal;
import java.util.Optional;

import cerne.members.Member;

/** Per-transfer limit to another bank; empty = no limit (special agreement). */
@FunctionalInterface
public interface LimitPolicy {

    Optional<BigDecimal> limitPerTransfer(Member member);
}
