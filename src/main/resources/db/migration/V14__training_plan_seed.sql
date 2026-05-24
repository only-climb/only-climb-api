-- =============================================================================
-- V14 — Seed one platform training plan (with a supporting platform workout
-- template). Spanish is the default locale (required), English supplied as a
-- secondary translation.
-- =============================================================================

-- Supporting platform workout template -----------------------------------------

INSERT INTO workout_templates (
    uuid, source, owner_id, visibility, difficulty_level,
    estimated_duration_minutes, target_discipline
) VALUES (
    '11111111-1111-1111-1111-000000000001'::uuid,
    'PLATFORM', NULL, 'PUBLIC', 'INTERMEDIATE',
    45, 'SPORT'
);

INSERT INTO workout_template_translations (template_id, locale, field, value)
SELECT id, 'es', 'name', 'Sesión de fuerza de dedos'
FROM workout_templates WHERE uuid = '11111111-1111-1111-1111-000000000001'::uuid;

INSERT INTO workout_template_translations (template_id, locale, field, value)
SELECT id, 'es', 'description',
       'Sesión de hangboard centrada en colgadas máximas en regleta de 20 mm.'
FROM workout_templates WHERE uuid = '11111111-1111-1111-1111-000000000001'::uuid;

INSERT INTO workout_template_translations (template_id, locale, field, value)
SELECT id, 'en', 'name', 'Finger strength session'
FROM workout_templates WHERE uuid = '11111111-1111-1111-1111-000000000001'::uuid;

INSERT INTO workout_template_translations (template_id, locale, field, value)
SELECT id, 'en', 'description',
       'Hangboard session focused on maximal hangs on a 20 mm edge.'
FROM workout_templates WHERE uuid = '11111111-1111-1111-1111-000000000001'::uuid;

-- Platform training plan -------------------------------------------------------

INSERT INTO training_plans (
    uuid, source, generation_type, owner_id, visibility,
    difficulty_level, target_discipline, primary_goal_type_id,
    duration_weeks, sessions_per_week, avg_session_duration_minutes,
    training_volume,
    requires_hangboard, requires_campus_board, requires_gym_access,
    requires_outdoor_climbing, is_recovery_focused,
    published_at
)
SELECT
    '22222222-2222-2222-2222-000000000001'::uuid,
    'PLATFORM', 'MANUAL', NULL, 'PUBLIC',
    'INTERMEDIATE', 'SPORT', gt.id,
    8, 3, 60,
    'MODERATE',
    TRUE, FALSE, FALSE, FALSE, FALSE,
    NOW()
FROM goal_types gt WHERE gt.code = 'FINGER_STRENGTH';

-- Plan translations ------------------------------------------------------------

INSERT INTO training_plan_translations (plan_id, locale, field, value)
SELECT id, 'es', 'name', 'Bloque de fuerza de dedos — 8 semanas'
FROM training_plans WHERE uuid = '22222222-2222-2222-2222-000000000001'::uuid;

INSERT INTO training_plan_translations (plan_id, locale, field, value)
SELECT id, 'es', 'short_description',
       'Plan de 8 semanas para aumentar la fuerza máxima de dedos.'
FROM training_plans WHERE uuid = '22222222-2222-2222-2222-000000000001'::uuid;

INSERT INTO training_plan_translations (plan_id, locale, field, value)
SELECT id, 'es', 'description',
       'Programa de hangboard con tres sesiones semanales en regleta de 20 mm. '
       || 'Combina colgadas máximas y repetidores. Recomendado tras una temporada de base.'
FROM training_plans WHERE uuid = '22222222-2222-2222-2222-000000000001'::uuid;

INSERT INTO training_plan_translations (plan_id, locale, field, value)
SELECT id, 'es', 'methodology',
       'Bloque de 7 segundos máximos con recuperación completa. '
       || 'La carga progresa cada dos semanas. Una semana es de descarga.'
FROM training_plans WHERE uuid = '22222222-2222-2222-2222-000000000001'::uuid;

INSERT INTO training_plan_translations (plan_id, locale, field, value)
SELECT id, 'en', 'name', 'Finger strength block — 8 weeks'
FROM training_plans WHERE uuid = '22222222-2222-2222-2222-000000000001'::uuid;

INSERT INTO training_plan_translations (plan_id, locale, field, value)
SELECT id, 'en', 'short_description',
       '8-week programme to increase maximum finger strength.'
FROM training_plans WHERE uuid = '22222222-2222-2222-2222-000000000001'::uuid;

INSERT INTO training_plan_translations (plan_id, locale, field, value)
SELECT id, 'en', 'description',
       'Hangboard programme with three weekly sessions on a 20 mm edge. '
       || 'Combines max hangs and repeaters. Recommended after a base season.'
FROM training_plans WHERE uuid = '22222222-2222-2222-2222-000000000001'::uuid;

INSERT INTO training_plan_translations (plan_id, locale, field, value)
SELECT id, 'en', 'methodology',
       '7-second maximal block with full recovery. Load progresses every two weeks. '
       || 'One week is a deload.'
FROM training_plans WHERE uuid = '22222222-2222-2222-2222-000000000001'::uuid;

-- Equipment requirement --------------------------------------------------------

INSERT INTO training_plan_equipment (plan_id, equipment_id, is_optional)
SELECT tp.id, eq.id, FALSE
FROM training_plans tp, equipment eq
WHERE tp.uuid = '22222222-2222-2222-2222-000000000001'::uuid
  AND eq.code = 'HANGBOARD';

INSERT INTO training_plan_equipment (plan_id, equipment_id, is_optional)
SELECT tp.id, eq.id, TRUE
FROM training_plans tp, equipment eq
WHERE tp.uuid = '22222222-2222-2222-2222-000000000001'::uuid
  AND eq.code = 'WEIGHTED_BELT';

-- Weeks ------------------------------------------------------------------------

INSERT INTO training_plan_weeks (uuid, plan_id, week_number, is_deload)
SELECT '33333333-3333-3333-3333-000000000001'::uuid, id, 1, FALSE
FROM training_plans WHERE uuid = '22222222-2222-2222-2222-000000000001'::uuid;

INSERT INTO training_plan_weeks (uuid, plan_id, week_number, is_deload)
SELECT '33333333-3333-3333-3333-000000000002'::uuid, id, 2, FALSE
FROM training_plans WHERE uuid = '22222222-2222-2222-2222-000000000001'::uuid;

-- Week translations ------------------------------------------------------------

INSERT INTO training_plan_week_translations (week_id, locale, field, value)
SELECT id, 'es', 'name', 'Semana 1 — Adaptación'
FROM training_plan_weeks WHERE uuid = '33333333-3333-3333-3333-000000000001'::uuid;

INSERT INTO training_plan_week_translations (week_id, locale, field, value)
SELECT id, 'es', 'focus', 'Adaptar el tejido conectivo a cargas máximas.'
FROM training_plan_weeks WHERE uuid = '33333333-3333-3333-3333-000000000001'::uuid;

INSERT INTO training_plan_week_translations (week_id, locale, field, value)
SELECT id, 'en', 'name', 'Week 1 — Adaptation'
FROM training_plan_weeks WHERE uuid = '33333333-3333-3333-3333-000000000001'::uuid;

INSERT INTO training_plan_week_translations (week_id, locale, field, value)
SELECT id, 'en', 'focus', 'Adapt connective tissue to maximal loads.'
FROM training_plan_weeks WHERE uuid = '33333333-3333-3333-3333-000000000001'::uuid;

INSERT INTO training_plan_week_translations (week_id, locale, field, value)
SELECT id, 'es', 'name', 'Semana 2 — Progresión'
FROM training_plan_weeks WHERE uuid = '33333333-3333-3333-3333-000000000002'::uuid;

INSERT INTO training_plan_week_translations (week_id, locale, field, value)
SELECT id, 'es', 'focus', 'Subir la carga manteniendo la calidad del agarre.'
FROM training_plan_weeks WHERE uuid = '33333333-3333-3333-3333-000000000002'::uuid;

INSERT INTO training_plan_week_translations (week_id, locale, field, value)
SELECT id, 'en', 'name', 'Week 2 — Progression'
FROM training_plan_weeks WHERE uuid = '33333333-3333-3333-3333-000000000002'::uuid;

INSERT INTO training_plan_week_translations (week_id, locale, field, value)
SELECT id, 'en', 'focus', 'Increase load while preserving grip quality.'
FROM training_plan_weeks WHERE uuid = '33333333-3333-3333-3333-000000000002'::uuid;

-- Sessions (one per week, day 2, pointing to the seeded workout template) -----

INSERT INTO training_plan_sessions (uuid, week_id, day_of_week, position, workout_template_id, is_optional)
SELECT '44444444-4444-4444-4444-000000000001'::uuid, w.id, 2, 1, t.id, FALSE
FROM training_plan_weeks w, workout_templates t
WHERE w.uuid = '33333333-3333-3333-3333-000000000001'::uuid
  AND t.uuid = '11111111-1111-1111-1111-000000000001'::uuid;

INSERT INTO training_plan_sessions (uuid, week_id, day_of_week, position, workout_template_id, is_optional)
SELECT '44444444-4444-4444-4444-000000000002'::uuid, w.id, 2, 1, t.id, FALSE
FROM training_plan_weeks w, workout_templates t
WHERE w.uuid = '33333333-3333-3333-3333-000000000002'::uuid
  AND t.uuid = '11111111-1111-1111-1111-000000000001'::uuid;

INSERT INTO training_plan_session_translations (session_id, locale, field, value)
SELECT id, 'es', 'notes', 'Calienta 15 minutos antes de las colgadas.'
FROM training_plan_sessions WHERE uuid = '44444444-4444-4444-4444-000000000001'::uuid;

INSERT INTO training_plan_session_translations (session_id, locale, field, value)
SELECT id, 'en', 'notes', 'Warm up 15 minutes before max hangs.'
FROM training_plan_sessions WHERE uuid = '44444444-4444-4444-4444-000000000001'::uuid;
