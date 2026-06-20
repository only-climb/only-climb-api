package app.onlyclimb.api.infrastructure.adapter.out.persistence.subscription;

import app.onlyclimb.api.domain.model.BillingPeriod;
import app.onlyclimb.api.domain.model.SubscriptionPlan;
import app.onlyclimb.api.domain.port.out.SubscriptionPlanRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SubscriptionPlanJpaAdapter implements SubscriptionPlanRepository {

    private final SpringDataSubscriptionPlanRepository springRepo;

    @Override
    public List<SubscriptionPlan> findByTierCode(String tierCode) {
        return springRepo.findByTierCode(tierCode).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SubscriptionPlan> findAllActive() {
        return springRepo.findAllActive().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SubscriptionPlan> findById(UUID id) {
        return springRepo.findByUuid(id).map(this::toDomain);
    }

    private SubscriptionPlan toDomain(SubscriptionPlanJpaEntity entity) {
        return new SubscriptionPlan(
                entity.getUuid(),
                entity.getTier().getUuid(),
                entity.getTier().getCode(),
                entity.getBillingPeriod() != null ? entity.getBillingPeriod() : BillingPeriod.LIFETIME,
                entity.getPriceCents(),
                entity.getCurrency(),
                entity.getExternalRef(),
                entity.isActive());
    }
}
