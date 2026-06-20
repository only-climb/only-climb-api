-- =============================================================================
-- V16 — Subscription tier translations (ES + EN)
-- =============================================================================
-- Marketing copy for each tier. Each tier has name, tagline, and description
-- in both English and Spanish (the project's default locale).

-- FREE tier
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'en', 'name', 'Free'
FROM subscription_tiers WHERE code = 'FREE';
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'en', 'tagline', 'Start your climbing journey'
FROM subscription_tiers WHERE code = 'FREE';
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'en', 'description', 'Access the platform exercise catalog, create one training plan, three workout templates, five custom exercises, and log up to 30 sessions. Community features included.'
FROM subscription_tiers WHERE code = 'FREE';

INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'es', 'name', 'Gratis'
FROM subscription_tiers WHERE code = 'FREE';
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'es', 'tagline', 'Empieza tu viaje en la escalada'
FROM subscription_tiers WHERE code = 'FREE';
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'es', 'description', 'Accede al catálogo de ejercicios de la plataforma, crea un plan de entrenamiento, tres plantillas de workout, cinco ejercicios personalizados y registra hasta 30 sesiones. Funcionalidades de comunidad incluidas.'
FROM subscription_tiers WHERE code = 'FREE';

-- BASIC tier
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'en', 'name', 'Basic'
FROM subscription_tiers WHERE code = 'BASIC';
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'en', 'tagline', 'Train smarter with AI-powered plans'
FROM subscription_tiers WHERE code = 'BASIC';
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'en', 'description', 'Everything in Free, plus: 3 training plans, 10 workout templates, 20 custom exercises, 3 active goals, unlimited workout logging, 10 assessment results with trend view, 5 AI plan generations per month, and CSV export.'
FROM subscription_tiers WHERE code = 'BASIC';

INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'es', 'name', 'Básico'
FROM subscription_tiers WHERE code = 'BASIC';
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'es', 'tagline', 'Entrena de forma inteligente con planes generados por IA'
FROM subscription_tiers WHERE code = 'BASIC';
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'es', 'description', 'Todo lo de Gratis, más: 3 planes de entrenamiento, 10 plantillas de workout, 20 ejercicios personalizados, 3 objetivos activos, registro ilimitado de sesiones, 10 evaluaciones con vista de tendencias, 5 generaciones de planes con IA al mes y exportación CSV.'
FROM subscription_tiers WHERE code = 'BASIC';

-- PREMIUM tier
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'en', 'name', 'Premium'
FROM subscription_tiers WHERE code = 'PREMIUM';
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'en', 'tagline', 'Unlock your full climbing potential'
FROM subscription_tiers WHERE code = 'PREMIUM';
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'en', 'description', 'Everything in Basic, plus: unlimited training plans, workout templates, custom exercises, and assessment results. 5 active goals, 20 AI plan generations per month, advanced analytics with charts, training calendar, and CSV + PDF export. Priority support included.'
FROM subscription_tiers WHERE code = 'PREMIUM';

INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'es', 'name', 'Premium'
FROM subscription_tiers WHERE code = 'PREMIUM';
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'es', 'tagline', 'Desbloquea todo tu potencial como escalador'
FROM subscription_tiers WHERE code = 'PREMIUM';
INSERT INTO subscription_tier_translations (tier_id, locale, field, value)
SELECT id, 'es', 'description', 'Todo lo de Básico, más: planes de entrenamiento, plantillas de workout, ejercicios personalizados y evaluaciones ilimitados. 5 objetivos activos, 20 generaciones de planes con IA al mes, analíticas avanzadas con gráficos, calendario de entrenamiento y exportación CSV + PDF. Soporte prioritario incluido.'
FROM subscription_tiers WHERE code = 'PREMIUM';
