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

**i18n:** name and description are translatable (platform exercises have EN + ES minimum). User-created exercises are stored in whatever language the user writes — no translation is applied.

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

- Identity is managed externally by **Clerk**.
- The `userId` (Clerk's subject) arrives in the JWT as the `sub` claim.
- **Never store passwords.** The API trusts the JWT issued by Clerk.
- User roles (`USER`, `ADMIN`) are stored in our database, not in the JWT.
- Always treat `userId` as an opaque string — never cast to integer.
- `UserProfile` stores extended data: weight (kg), height (cm), primary discipline, active goal.
- **Weight is required** for hangboard-related plan generation — strength-to-weight ratio is a core metric in climbing. Block AI plan generation if weight is not set.

---

## Subscription & Feature Gating

| Feature | Free | Premium |
|---|---|---|
| Log workout sessions | ✅ | ✅ |
| Access platform exercises | ✅ | ✅ |
| Basic platform workout templates | ✅ | ✅ |
| All platform content | ❌ | ✅ |
| Create & share exercises | ✅ (limited) | ✅ (unlimited) |
| AI plan generation | ❌ | ✅ |
| Advanced analytics & progress charts | ❌ | ✅ |
| Training groups | ❌ | ✅ |

Subscription check must happen in the **application service** before invoking premium use cases. Never check subscription in the domain layer.

---

## Internationalization (i18n)

| Content type | Languages | Who provides translations |
|---|---|---|
| Platform exercises, plans, tests | EN + ES minimum | Platform admin |
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

## What NOT to Do

- Do not add multi-sport support — scope is climbing only.
- Do not store passwords or auth tokens — delegate entirely to Clerk.
- Do not put subscription logic in the domain layer — it belongs in the application service.
- Do not put i18n columns directly on entity tables (`name_en`, `name_es`) — use a translations table.
- Do not make `AssessmentResult` mutable — it is a historical record.
- Do not expose internal Long IDs in the API — always use UUIDs externally.
