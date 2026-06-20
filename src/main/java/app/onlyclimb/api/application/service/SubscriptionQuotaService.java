package app.onlyclimb.api.application.service;

import app.onlyclimb.api.domain.exception.QuotaExceededException;
import app.onlyclimb.api.domain.exception.SubscriptionNotFoundException;
import app.onlyclimb.api.domain.model.FeatureLimits;
import app.onlyclimb.api.domain.model.SubscriptionPlan;
import app.onlyclimb.api.domain.model.UserSubscription;
import app.onlyclimb.api.domain.port.out.SubscriptionPlanRepository;
import app.onlyclimb.api.domain.port.out.UserSubscriptionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Queries the user's current subscription and enforces tier-specific limits.
 * Called by application services before creating resources.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionQuotaService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;

    /** Returns the FeatureLimits for the user's current tier. */
    public FeatureLimits getLimits(UUID userId) {
        String tierCode = getTierCode(userId);
        return FeatureLimits.forTier(tierCode);
    }

    /** Returns the tier code of the user's active subscription. */
    public String getTierCode(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .map(sub -> planRepository.findById(sub.getPlanId())
                        .map(SubscriptionPlan::getTierCode)
                        .orElse("FREE"))
                .orElse("FREE");
    }

    // ─── Assertion methods ──────────────────────────────────────────────────

    public void assertCanCreateTrainingPlan(UUID userId, int currentCount) {
        FeatureLimits limits = getLimits(userId);
        if (!limits.isUnlimited(limits.getMaxTrainingPlans())
                && currentCount >= limits.getMaxTrainingPlans()) {
            throw new QuotaExceededException("training_plans",
                    limits.getMaxTrainingPlans(), currentCount);
        }
    }

    public void assertCanCreateWorkoutTemplate(UUID userId, int currentCount) {
        FeatureLimits limits = getLimits(userId);
        if (!limits.isUnlimited(limits.getMaxWorkoutTemplates())
                && currentCount >= limits.getMaxWorkoutTemplates()) {
            throw new QuotaExceededException("workout_templates",
                    limits.getMaxWorkoutTemplates(), currentCount);
        }
    }

    public void assertCanCreateExercise(UUID userId, int currentCount) {
        FeatureLimits limits = getLimits(userId);
        if (!limits.isUnlimited(limits.getMaxExercises())
                && currentCount >= limits.getMaxExercises()) {
            throw new QuotaExceededException("exercises",
                    limits.getMaxExercises(), currentCount);
        }
    }

    public void assertCanCreateGoal(UUID userId, int currentActiveCount) {
        FeatureLimits limits = getLimits(userId);
        if (!limits.isUnlimited(limits.getMaxActiveGoals())
                && currentActiveCount >= limits.getMaxActiveGoals()) {
            throw new QuotaExceededException("active_goals",
                    limits.getMaxActiveGoals(), currentActiveCount);
        }
    }

    public void assertCanCreateWorkoutLog(UUID userId, int currentTotalCount) {
        FeatureLimits limits = getLimits(userId);
        if (!limits.isUnlimited(limits.getMaxWorkoutLogs())
                && currentTotalCount >= limits.getMaxWorkoutLogs()) {
            throw new QuotaExceededException("workout_logs",
                    limits.getMaxWorkoutLogs(), currentTotalCount);
        }
    }

    public void assertCanRecordAssessment(UUID userId, int currentCount) {
        FeatureLimits limits = getLimits(userId);
        if (!limits.isUnlimited(limits.getMaxAssessmentResults())
                && currentCount >= limits.getMaxAssessmentResults()) {
            throw new QuotaExceededException("assessment_results",
                    limits.getMaxAssessmentResults(), currentCount);
        }
    }

    public void assertCanGenerateAiPlan(UUID userId, int usedThisMonth) {
        FeatureLimits limits = getLimits(userId);
        if (limits.getAiGenerationsPerMonth() == 0) {
            throw new QuotaExceededException("ai_plan_generations", 0, usedThisMonth);
        }
        if (!limits.isUnlimited(limits.getAiGenerationsPerMonth())
                && usedThisMonth >= limits.getAiGenerationsPerMonth()) {
            throw new QuotaExceededException("ai_plan_generations",
                    limits.getAiGenerationsPerMonth(), usedThisMonth);
        }
    }
}
