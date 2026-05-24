package app.onlyclimb.api.domain.model;

/**
 * How a {@link TrainingPlan} came into existence.
 *
 * <ul>
 *   <li>{@link #MANUAL} — authored by a human (platform admin or end-user).</li>
 *   <li>{@link #AI_GENERATED} — produced by an external LLM via the AI
 *       generation job pipeline.</li>
 *   <li>{@link #FORKED} — created by deep-copying another plan.</li>
 * </ul>
 */
public enum PlanGenerationType {
    MANUAL,
    AI_GENERATED,
    FORKED
}
