-- =============================================================================
-- V13 — Localisation fixes for platform assessment seed.
--
--   1. Renames the seeded definition code so it does not reference a
--      third-party brand (Lattice). The new code is neutral.
--   2. Adds the Spanish translations that were missing in V12. Spanish is
--      the default project locale; English remains as a secondary fallback.
--   3. Updates the user-profile default locale to Spanish for new sign-ups.
-- =============================================================================

-- 1. Rename the definition code -------------------------------------------------

UPDATE assessment_definitions
SET    code = 'FINGER_STRENGTH_V1'
WHERE  code = 'LATTICE_FINGER_STRENGTH_V1';

-- 2. Spanish translations for the definition -----------------------------------

INSERT INTO assessment_definition_translations (definition_id, locale, field, value)
SELECT id, 'es', 'name', 'Fuerza de dedos'
FROM   assessment_definitions WHERE code = 'FINGER_STRENGTH_V1';

INSERT INTO assessment_definition_translations (definition_id, locale, field, value)
SELECT id, 'es', 'description',
       'Mide la fuerza absoluta y relativa de los dedos sobre un canto de 20 mm.'
FROM   assessment_definitions WHERE code = 'FINGER_STRENGTH_V1';

INSERT INTO assessment_definition_translations (definition_id, locale, field, value)
SELECT id, 'es', 'protocol',
       'Calienta 15 minutos. Realiza tres suspensiones máximas de 7 segundos con recuperación completa entre series.'
FROM   assessment_definitions WHERE code = 'FINGER_STRENGTH_V1';

-- 3. Spanish translations for the tests ----------------------------------------

INSERT INTO assessment_test_translations (test_id, locale, field, value)
SELECT t.id, 'es', 'name', n.name
FROM   assessment_tests t
JOIN   assessment_definitions d ON d.id = t.definition_id
JOIN (VALUES
    ('MAX_HANG_20MM_HALF_CRIMP', 'Suspensión máxima 20 mm — semi-arqueo'),
    ('MAX_HANG_20MM_OPEN_HAND',  'Suspensión máxima 20 mm — mano abierta'),
    ('BODYWEIGHT',               'Peso corporal')
) AS n(code, name) ON n.code = t.code
WHERE d.code = 'FINGER_STRENGTH_V1';

INSERT INTO assessment_test_translations (test_id, locale, field, value)
SELECT t.id, 'es', 'protocol', p.protocol
FROM   assessment_tests t
JOIN   assessment_definitions d ON d.id = t.definition_id
JOIN (VALUES
    ('MAX_HANG_20MM_HALF_CRIMP',
     'Añade lastre progresivamente. Registra la carga máxima sostenida durante 7 segundos.'),
    ('MAX_HANG_20MM_OPEN_HAND',
     'Añade lastre progresivamente. Registra la carga máxima sostenida durante 7 segundos.'),
    ('BODYWEIGHT',
     'Peso corporal en el momento del test, usado para normalizar el resto de mediciones.')
) AS p(code, protocol) ON p.code = t.code
WHERE d.code = 'FINGER_STRENGTH_V1';

-- 4. Default locale for new user profiles --------------------------------------

ALTER TABLE user_profiles
    ALTER COLUMN locale SET DEFAULT 'es';
