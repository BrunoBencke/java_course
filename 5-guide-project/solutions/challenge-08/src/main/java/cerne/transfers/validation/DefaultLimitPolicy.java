package cerne.transfers.validation;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import cerne.members.Member;

/** Individual (and rural producer) R$ 5.000; legal entity (and association) R$ 50.000; special agreement with no limit. */
@ApplicationScoped
public class DefaultLimitPolicy implements LimitPolicy {

    private static final BigDecimal INDIVIDUAL = new BigDecimal("5000.00");
    private static final BigDecimal LEGAL_ENTITY = new BigDecimal("50000.00");

    @Override
    public Optional<BigDecimal> limitPerTransfer(Member member) {
        if (member.hasSpecialAgreement()) {
            return Optional.empty();
        }
        return Optional.of(switch (member.getType()) {
            case INDIVIDUAL, RURAL_PRODUCER -> INDIVIDUAL;
            case LEGAL_ENTITY, ASSOCIATION -> LEGAL_ENTITY;
        });
    }
}
