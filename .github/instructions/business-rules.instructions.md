---
description: "Use when implementing any feature, endpoint, or domain entity. Contains the business rules, domain concepts, and key design decisions specific to the Only Climb application. Read this before generating any domain model, use case, or API endpoint."
applyTo: "src/main/**"
---

# Only Climb — Business Rules & Domain Knowledge

## What is Only Climb?

An AI-powered climbing training platform. Climbers assess their current physical level through standardized tests, set goals, and receive personalized training plans (AI-generated or platform-curated). They log sessions, track progress over time, and can train with the community.

Reference products: Lattice Training, Climbro.

---

## Sport Scope

**Only climbing** — no generic multi-sport support. Climbing disciplines in scope:
- Sport / Lead
- Boulder
- Trad (future)
- Fingerboard / Hangboard specific training

---

## Core Domain Concepts

### Exercise

The atomic training unit.

**Source types:**
- `PLATFORM` — curated by the platform admin. Immutable. Cannot be edited or deleted by users.
- `USER_CREATED` — created by a user. Mutable by owner. Visibility: `PRIVATE` (default) or `PUBLIC`.

**Climbing-specific categories:** `HANGBOARD`, `PULL`, `CORE`, `ANTAGONIST`, `FLEXIBILITY`, `ENDURANCE`, `TECHNIQUE`

**Parameter types** — each exercise defines which parameters apply to it:
| Parameter | Description |
|---|---|
| `REPS` | Number of repetitions |
| `SETS` | Number of sets/rounds |
| `REST_SECONDS` | Rest between sets |
| `DURATION_SECONDS` | Time under tension (key for hangboard holds) |
| `WEIGHT_KG` | Added or assisted weight |
| `INTENSITY_PERCENT` | % of max effort (hangboard protocols) |
| `EDGE_DEPTH_MM` | Edge depth in mm (hangboard-specific) |
| `GRIP_TYPE` | `CRIMP`, `OPEN_HAND`, `HALF_CRIMP`, `PINCH`, `SLOPER`, `MONO` |
| `RPE` | Rate of Perceived Exertion (1–10) |

**i18n:** name and description are translatable (platform exercises have ES + EN minimum — Spanish is the default project locale). User-created exercises are stored in whatever language the user writes — no translation is applied.

### WorkoutTemplate

A named, ordered collection of exercises with their configuration. Equivalent to a "training session".

**Source types:** `PLATFORM` (immutable) or `USER_CREATED` (mutable).

A user can **fork** a platform template → creates an independent `USER_CREATED` copy. Changes to the original do NOT propagate to forks.

Visibility for user templates: `PRIVATE` | `PUBLIC`.

**i18n:** name, description, and exercise notes are translatable for platform content.

### TrainingPlan

A multi-week program that schedules `WorkoutTemplate`s across days and weeks with a defined goal.

**Source types:** `PLATFORM` (immutable) or `USER_CREATED` (mutable, including AI-generated plans).

A user can **fork** a platform plan → creates an independent `USER_CREATED` copy, modifiable at will.

**Fork invariant:** A fork is completely independent. Modifications to the platform plan never affect existing forks.

Users can share their `USER_CREATED` plans publicly. Sharing can be revoked at any time — the plan becomes private again, but forks others have already made remain as independent copies.

**i18n:** name, description, goal label for platform plans only.

### AssessmentDefinition

A standardized test battery for measuring a climber's current physical profile. Defined by the platform admin. Multiple types exist (specific tests TBD):
- Finger strength (max hang protocols on various edges)
- Crimp vs. open-hand comparison
- Pull-up max reps
- Repeater endurance protocols
- Pinch strength
- Body composition (weight/height ratio for strength-to-weight)

Each test definition specifies: what metric it produces, unit, and how to interpret the result.

### AssessmentResult

A completed assessment by a user. **Immutable once saved** — it is a historical record. To update, the user must take a new assessment.

Contains individual `AssessmentMetric` entries (e.g., `max_hang_20mm_crimp = 25 kg`).

AssessmentResults feed:
1. **Level calculation** — derives current climbing potential (grade)
2. **AI plan generation** — provides the input profile for the AI
3. **Progress tracking** — trend analysis over time

### Goal

Structured training objective. Not free text — it must be a recognized type so the AI can use it.

Goal types:
- `FINGER_STRENGTH` — Improve maximum finger strength
- `POWER_ENDURANCE` — Improve resistance to pump (power endurance)
- `AEROBIC_BASE` — Build aerobic base for endurance
- `GRADE_TARGET` — Send a specific grade (includes a target grade value)
- `ANTAGONIST` — Injury prevention, shoulder and wrist balance
- `GENERAL_STRENGTH` — Pull strength, body tension

A user has one active goal per training cycle.

### AI-Generated Plan

A `TrainingPlan USER_CREATED` produced by AI on behalf of the user.

**Required inputs:**
- Latest `AssessmentResult` (at least one must exist — hard prerequisite)
- Active `Goal`
- Schedule constraints: available training days per week, max session duration
- Active subscription (AI generation is a premium feature)

The AI (external LLM via API, e.g., OpenAI with structured output) receives the assessment metrics, goal, and constraints, and returns a structured plan. This plan is persisted as a regular `USER_CREATED TrainingPlan`.

**Async execution model.** LLM calls take 10–60s and must not block HTTP requests. The flow is job-based:

1. Client calls `POST /api/v1/ai-plans`. Application service:
   - Validates premium subscription + assessment + goal + weight.
   - Rejects if the user already has a `PENDING`/`RUNNING` job (partial unique index enforces this at DB level).
   - Builds the immutable `input_payload` snapshot (schedule constraints + assessment metrics + goal + user profile fields).
   - Inserts an `ai_plan_generation_jobs` row with `status = PENDING` and returns its UUID.
2. A worker picks the job, calls the AI provider via `AiPlanGeneratorPort` (output port), and updates the row: `started_at`, `status = RUNNING`, then `model`, `prompt_version`, `external_request_id`, token usage and `cost_micros`.
3. On success: persists the resulting `TrainingPlan USER_CREATED` (`generation_type = AI_GENERATED`) and sets `resulting_plan_id`, `status = SUCCEEDED`, `finished_at`, `raw_response`.
4. On failure: records `error_code`, `error_message`, `status = FAILED`, `finished_at`. Client can retry — the new job stores the old job id in `retry_of_job_id`.
5. Client polls `GET /api/v1/ai-plans/jobs/{uuid}` (or subscribes via SSE) until terminal status.

**Provider abstraction.** `ai_provider` is a Postgres enum (`OPENAI` today). Domain defines `AiPlanGeneratorPort` with `generate(input)` returning a structured plan; provider-specific code (OpenAI client, structured output schema, retry policy) lives only in the infrastructure adapter. Switching to Anthropic or a local model is an adapter swap.

**Auditing & quota.** Every job records `prompt_tokens`, `completion_tokens`, `total_tokens`, `cost_micros`, `cost_currency`, `model`, `prompt_version`. Use these to: bill internally, enforce monthly quota per subscription, A/B test prompt versions, and reproduce a generation from `input_payload` without re-billing.

### WorkoutLog

A record of a completed training session. Can follow a `WorkoutTemplate` or be ad-hoc. If following a plan, references the specific plan week and day.

Contains: date, total duration, overall RPE, free notes.

### WorkoutLogEntry

The actual performance of one exercise within a log session. Stores:
- Planned values (from the template, if any)
- Actual values performed
- Status: `COMPLETED`, `SKIPPED`, `MODIFIED`

---

## Community Features

### Content Sharing
- `Exercise` and `WorkoutTemplate` with visibility `PUBLIC` are discoverable by all users.
- Owner can revert to `PRIVATE` at any time. Existing forks are unaffected.

### TrainingGroup
- A group of climbers training together.
- Visibility: `PUBLIC` | `INVITE_ONLY`
- Roles within group: `ADMIN`, `MEMBER`
- Can have a shared training plan
- A user can belong to multiple groups
- Leaving a group does not delete the user's workout logs

### Social Graph
- A user can follow other users (`UserFollowing`)
- Activity feed: completed sessions, new shared plans, group activity (scope TBD)

---

## Identity & Authentication

- Identity is owned by an **external auth provider**. Today: Clerk. The pair `(auth_provider, external_user_id)` is the stable foreign identity. The DB schema must never assume a single provider — use the `auth_provider` enum so we can add Auth0/Supabase/etc. without schema changes.
- The JWT issued by the provider arrives in every request. The `sub` claim becomes `external_user_id`. Combined with the configured `auth_provider`, we resolve the local `users` row.
- **Never store passwords.** The API trusts the JWT issued by the auth provider.
- User roles (`USER`, `ADMIN`) are stored in our database, not in the JWT.
- Always treat `external_user_id` as an opaque string — never cast to integer.
- `UserProfile` stores extended data: weight (kg), height (cm), primary discipline, preferred locale.
- **Weight is required** for hangboard-related plan generation — strength-to-weight ratio is a core metric in climbing. Block AI plan generation if weight is not set.

---

## Subscription & Feature Gating

### Model
- **Tier** (`subscription_tiers`): catalog of product levels (`FREE`, `BASIC`, `PREMIUM`, ...). Translatable marketing copy lives in `subscription_tier_translations`.
- **Plan** (`subscription_plans`): a concrete SKU = `tier × billing_period × currency × price`. Billing periods: `MONTHLY`, `YEARLY`, `LIFETIME` (used for the free tier).
- **PaymentCustomer** (`payment_customers`): canonical mapping `(user, payment_provider) → external_customer_id`. Stable across plan changes — Stripe customer id lives here, never on subscription rows.
- **UserSubscription** (`user_subscriptions`): a user's current state for a plan. Stripe-style fields: `status`, `current_period_start/end`, `trial_ends_at`, `cancel_at_period_end`, `payment_provider`, `external_subscription_id`. `payment_provider` is `NULL` for the internal free plan (no gateway involved).
- **SubscriptionInvoice** (`subscription_invoices`): historical billing record per invoice issued by the provider. Used for billing history UI, refunds and accounting.
- **PaymentWebhookEvent** (`payment_webhook_events`): inbound provider events. `UNIQUE(provider, external_event_id)` gives at-most-once processing semantics — duplicated deliveries are no-ops.
- Statuses: `TRIALING`, `ACTIVE`, `PAST_DUE`, `CANCELLED`, `EXPIRED`. A user can have at most one non-terminal subscription at a time (enforced by partial unique index).
- Plan changes create a **new** `user_subscriptions` row; the previous row transitions to `CANCELLED`. Renewals update `current_period_*` on the same row.

### Payment provider abstraction (hexagonal)
- Today's gateway is **Stripe**. The DB schema must not lock to it.
- `payment_provider` is a Postgres enum (`STRIPE` today) — adding `PADDLE`, `LEMONSQUEEZY`, etc. is a one-line `ALTER TYPE`.
- All external references are namespaced by provider: `(payment_provider, external_subscription_id)`, `(payment_provider, external_invoice_id)`, `(payment_provider, external_customer_id)`.
- Domain layer defines a `PaymentGatewayPort` output port with operations like `createCheckoutSession`, `cancelAtPeriodEnd`, `openCustomerPortal`. Stripe-specific code lives only in the adapter.
- Webhooks land on a dedicated controller (no auth — verified by signature). The controller persists the raw event in `payment_webhook_events`, then dispatches to an application service that mutates `user_subscriptions` / `subscription_invoices`.

### Feature gating
| Feature | Free | Basic | Premium |
|---|---|---|---|
| Log workout sessions | ✅ | ✅ | ✅ |
| Access platform exercises | ✅ | ✅ | ✅ |
| Basic platform workout templates | ✅ | ✅ | ✅ |
| All platform content | ❌ | ✅ | ✅ |
| Create & share exercises | ✅ (limited) | ✅ | ✅ (unlimited) |
| AI plan generation | ❌ | ❌ | ✅ |
| Advanced analytics & progress charts | ❌ | ✅ | ✅ |
| Training groups | ❌ | ❌ | ✅ |

Subscription check must happen in the **application service** before invoking premium use cases. Never check subscription in the domain layer. Resolve the user's effective tier via their active `UserSubscription → Plan → Tier`.

### Provisioning invariants
- Every active user has **at least one** `user_subscriptions` row. On first sign-up, the application service inserts a row pointing to the FREE/LIFETIME plan with `status = ACTIVE` and `payment_provider = NULL`.
- Downgrade on cancellation: when a paid subscription transitions to `EXPIRED`, the application service must insert a new FREE row so the user is never left without a tier.

---

## Internationalization (i18n)

| Content type | Languages | Who provides translations |
|---|---|---|
| Platform exercises, plans, tests | ES + EN minimum (ES is the default) | Platform admin |
| User-created content | User's own language | Not translated |
| UI strings | Separate concern — not in DB | Frontend |

**DB pattern:** use a separate translations table per translatable entity (e.g., `exercise_translations` with `locale`, `field`, `value`). **Never use `name_en`, `name_es` columns** — it does not scale.

---

## Grade System

- Use the **French/European sport climbing scale**: `3`, `4`, `4+`, `5`, `5+`, `6a`, `6a+`, `6b`, ..., `9c`.
- Store grades as strings validated against a catalog — never as enums (the scale can evolve).
- Bouldering: Fontainebleau scale (`3`, `4`, ..., `8C+`).
- `GradeTarget` in a `GRADE_TARGET` goal must be a valid grade from the appropriate scale.

---

## Key Business Invariants

| Rule | Enforced where |
|---|---|
| Platform content is immutable | Application service (reject mutation commands) |
| Fork creates a fully independent copy | Application service (deep copy on fork) |
| AssessmentResult is immutable once saved | Domain (no setter methods, no update use case) |
| AI plan generation requires ≥1 completed assessment | Application service (pre-condition check) |
| AI plan generation requires active subscription | Application service (pre-condition check) |
| Weight required for hangboard plan generation | Application service (pre-condition check) |
| Shared content can be revoked; forks are unaffected | Application service (update visibility, do not cascade) |
| Grade values must belong to the valid scale catalog | Domain — `ClimbingGrade` value object validates on construction |

---

## API Design Rules

- All endpoints are prefixed with `/api/v1/`.
- Use **UUIDs** in all public API identifiers — never expose internal Long PKs.
- Endpoints that mutate state require authentication.
- Pagination: cursor-based for all list endpoints. Response shape: `{ data: [...], nextCursor: "..." }`.
- Language selection: `Accept-Language` header drives which translation locale is returned for platform content.

---

## Data Architecture Decisions (Confirmed)

### Identity Strategy
- **Public API**: always UUID (`java.util.UUID`). Exposed in all request/response DTOs.
- **Internal persistence**: surrogate Long PK for JPA performance (joins, indexes).
- **Mapping**: the JPA adapter maps Long ↔ UUID. The domain entity holds the UUID as its identity. Long is an infrastructure detail — never surfaces in domain or application layers.

```
Domain entity:  UUID id  ← the real identity
JPA entity:     Long id (PK, auto-generated) + UUID uuid (unique, indexed)
API response:   UUID only
```

### Internationalization (i18n) Strategy
Translatable content uses a **dedicated translations table per entity**, not columns per language.

**Pattern:**
```
exercise                         exercise_translation
────────────────────             ────────────────────────────────
id (Long, PK)                    id (Long, PK)
uuid (UUID, unique)              exercise_id (Long, FK → exercise.id)
category                         locale (VARCHAR, e.g. "en", "es")
...                              field  (VARCHAR, e.g. "name", "description")
                                 value  (TEXT)
```

**Rules:**
- Only PLATFORM content is translated by the platform. USER_CREATED content is stored as-is.
- Supported locales at launch: `es` (default, required, fallback), `en`.
- If a translation for the requested locale does not exist, fall back to `es`.
- The API reads the desired locale from the `Accept-Language` header.
- **Never add `name_en`, `name_es`, `description_en`, `description_es` columns** to entity tables — this does not scale and requires schema changes per new language.
- Entities that require translation: `Exercise`, `WorkoutTemplate`, `TrainingPlan`, `AssessmentDefinition`, `ExerciseCategory`, `GoalType`.

---

## What NOT to Do

- Do not add multi-sport support — scope is climbing only.
- Do not store passwords or auth tokens — delegate entirely to Clerk.
- Do not put subscription logic in the domain layer — it belongs in the application service.
- Do not put i18n columns directly on entity tables (`name_en`, `name_es`) — use a translations table.
- Do not make `AssessmentResult` mutable — it is a historical record.
- Do not expose internal Long IDs in the API — always use UUIDs externally.
- Do not use the domain entity's Long PK anywhere outside the persistence adapter.
