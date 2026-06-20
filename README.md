# 🧗 Only Climb API

> Plataforma de entrenamiento de escalada potenciada por IA — Backend API

[![Java](https://img.shields.io/badge/Java-25-red)](https://jdk.java.net/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue)](https://www.postgresql.org/)
[![CI](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF)](https://github.com/features/actions)
[![Release](https://img.shields.io/badge/Release-please-4285F4)](https://github.com/googleapis/release-please)
[![Docker](https://img.shields.io/badge/Docker-ghcr.io-2496ED)](https://github.com/features/packages)
[![Tests](https://img.shields.io/badge/tests-220%20✅-success)]()
[![License](https://img.shields.io/badge/license-Proprietary-lightgrey)]()

**Only Climb** ayuda a escaladores a evaluar su nivel físico mediante tests estandarizados, fijar objetivos, y recibir planes de entrenamiento personalizados (generados por IA o curados por la plataforma). Permite registrar sesiones, seguir el progreso y entrenar en comunidad.

---

## 🏗️ Arquitectura

Arquitectura **hexagonal (Ports & Adapters)** con 3 capas concéntricas:

```
infrastructure/ → application/ → domain/
    (Spring,       (orquestación,  (lógica pura,
     JPA, REST)     casos de uso)   sin frameworks)
```

- **`domain/`** — Entidades, value objects, puertos (interfaces), excepciones. **Cero dependencias de frameworks.**
- **`application/`** — Servicios que implementan los puertos de entrada, usando los de salida.
- **`infrastructure/`** — Spring MVC, Spring Data JPA, Spring Security, Flyway, configuración.

### Regla de oro
**El dominio no sabe nada de infraestructura.** No hay `@Service`, `@Repository`, ni `@Entity` dentro de `domain/`.

---

## 🛠️ Stack Tecnológico

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 25 | Lenguaje base |
| Spring Boot | 4.0.6 | Framework de aplicación |
| Spring MVC | 4.x | API REST |
| Spring Data JPA | 4.x | Persistencia |
| Spring Security | 4.x | Autenticación OAuth2 (JWT) |
| PostgreSQL | 17 | Base de datos |
| Flyway | 10.x | Migraciones (18 archivos) |
| Stripe | 33.x | Pasarela de pago, Checkout, Billing Portal, webhooks |
| Lombok | 1.18.x | Reducción de boilerplate |
| Springdoc OpenAPI | 2.8.x | Documentación Swagger |
| Testcontainers | 2.x | Tests de integración (PostgreSQL) |
| Docker Compose | — | Desarrollo local (PostgreSQL) |
| Docker | — | Imagen de producción en GHCR |
| GitHub Actions | — | CI/CD (verify + release-please + attach artifacts) |

---

## 🚀 Arranque rápido

### Requisitos
- Java 25
- Docker y Docker Compose
- Maven Wrapper (incluido: `./mvnw`)

### 1. Levantar PostgreSQL
```bash
docker compose up -d
```

### 2. Ejecutar la aplicación
```bash
export JAVA_HOME=$HOME/.local/jdks/jdk-25.0.3+9
export PATH=$JAVA_HOME/bin:$PATH
./mvnw spring-boot:run
```

La API arranca en `http://localhost:8080`.

### 3. Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### 4. Tests
```bash
./mvnw verify     # 220 tests
```

### 5. Construir imagen Docker (local)
```bash
./mvnw package -DskipTests
docker build -t only-climb-api .
docker run -p 8080:8080 only-climb-api
```

---

## 🔄 CI/CD Pipeline

El proyecto usa **GitHub Actions** con 3 workflows que automatizan el ciclo completo de desarrollo:

```mermaid
graph TD
    A[git push feat/fix] -->|PR a main| B[verify.yml]
    B -->|220 tests ✅| C{¿merge a main?}
    C -->|sí| D[release-please.yml 🤖]
    D -->|abre PR de release| E["PR: release v1.2.0"]
    E -->|merge manual| F["🏷️ tag v1.2.0 + GitHub Release"]
    F --> G[attach-artifacts.yml]
    G --> H[JAR + Docker image en GHCR]
```

| Workflow | Disparador | ¿Qué hace? |
|---|---|---|
| **`verify.yml`** | Push/PR a `main` | Build + 220 tests con Testcontainers + upload de reports |
| **`release-please.yml`** | Push a `main` | Crea/actualiza PR de release automático basado en [conventional commits](https://www.conventionalcommits.org/) |
| **`attach-artifacts.yml`** | Release publicado | Build + test + JAR adjunto al release + **Docker image a GHCR** |

### Versionado automático con Conventional Commits

El versionado sigue [SemVer](https://semver.org/) y se calcula automáticamente a partir de los mensajes de commit:

```bash
git commit -m "feat: añadir plan de entrenamiento IA"   # → bump MINOR (1.0 → 1.1)
git commit -m "fix: corregir NPE en login"              # → bump PATCH (1.1.0 → 1.1.1)
git commit -m "feat!: cambiar API de autenticación"     # → bump MAJOR (1.x → 2.0)
```

Al mergear a `main`, release-please abre un **PR de release** con el changelog y la versión propuesta. Al mergear ese PR, se crea automáticamente el tag, el GitHub Release, y se publica la imagen Docker en `ghcr.io/<usuario>/only-climb-api`.

### Flujo de trabajo diario

```bash
git checkout -b feat/nueva-funcionalidad
# ... desarrollar ...
git commit -m "feat: descripción del cambio"
git push origin feat/nueva-funcionalidad
# → Abrir PR → verify.yml corre tests → revisión → merge
# → release-please abre PR de release → merge → release 🚀
```

### Protección de rama recomendada

En **Settings → Branches → Add rule** para `main`:
- ☑ Require a pull request before merging
- ☑ Require status checks: `build-and-test`
- ☑ Require branches to be up to date
- ☑ Require conversation resolution

### Desplegar la imagen Docker

```bash
docker pull ghcr.io/<usuario>/only-climb-api:latest
docker run -p 8080:8080 ghcr.io/<usuario>/only-climb-api:latest
```

---

## 🔐 Autenticación

La API es **stateless** y confía en JWTs emitidos por **Clerk** (auth provider externo). No se almacenan contraseñas.

- Cada petición autenticada incluye un `Authorization: Bearer <jwt>`.
- Los usuarios se crean/actualizan/eliminan vía webhooks de Clerk (`POST /api/v1/webhooks/clerk`), verificados con firma **Svix**.
- Roles (`USER`, `ADMIN`) residen en la BD local, no en el JWT.
- Endpoints `GET` públicos (catálogos, ejercicios, plantillas, planes). El resto requiere autenticación.

---

## 📦 Funcionalidades

### ✅ Implementadas

#### 👤 Usuarios
| Endpoint | Descripción |
|---|---|
| `GET /api/v1/users/me` | Perfil del usuario autenticado |
| `GET /api/v1/users/me/profile` | Perfil extendido (peso, altura, disciplina, locale) |
| `PUT /api/v1/users/me/profile` | Actualizar perfil |
| `GET /api/v1/users/{id}` | Ver usuario por ID (admin o self) |
| `GET /api/v1/users/{id}/profile` | Perfil de otro usuario |
| `POST /api/v1/webhooks/clerk` | Webhook de Clerk (provisiona/actualiza/elimina usuarios) |

#### 🏋️ Ejercicios
| Endpoint | Descripción |
|---|---|
| `GET /api/v1/exercises` | Listar ejercicios (paginado, filtrable) |
| `GET /api/v1/exercises/{id}` | Ver ejercicio |
| `POST /api/v1/exercises` | Crear ejercicio (usuario) |
| `PUT /api/v1/exercises/{id}` | Editar ejercicio propio |
| `DELETE /api/v1/exercises/{id}` | Eliminar ejercicio propio |

Categorías: `HANGBOARD`, `PULL`, `CORE`, `ANTAGONIST`, `FLEXIBILITY`, `ENDURANCE`, `TECHNIQUE`.  
Parámetros: reps, sets, descanso, duración, peso, % intensidad, profundidad de regleta, tipo de agarre, RPE.

#### 📋 Workout Templates (sesiones de entrenamiento)
| Endpoint | Descripción |
|---|---|
| `GET /api/v1/workout-templates` | Listar plantillas |
| `GET /api/v1/workout-templates/{id}` | Ver plantilla |
| `POST /api/v1/workout-templates` | Crear plantilla |
| `PUT /api/v1/workout-templates/{id}` | Editar plantilla propia |
| `DELETE /api/v1/workout-templates/{id}` | Eliminar plantilla propia |
| `POST /api/v1/workout-templates/{id}/fork` | Forkear plantilla de plataforma |

#### 📅 Training Plans (planes multi-semana)
| Endpoint | Descripción |
|---|---|
| `GET /api/v1/training-plans` | Listar planes (con filtros avanzados) |
| `GET /api/v1/training-plans/{id}` | Ver plan |
| `POST /api/v1/training-plans` | Crear plan |
| `PUT /api/v1/training-plans/{id}` | Editar plan propio |
| `DELETE /api/v1/training-plans/{id}` | Eliminar plan propio |
| `POST /api/v1/training-plans/{id}/fork` | Forkear plan de plataforma |

#### 📊 Assessments (evaluaciones)
| Endpoint | Descripción |
|---|---|
| `GET /api/v1/assessments/definitions` | Definiciones de tests (plataforma) |
| `POST /api/v1/assessments/results` | Registrar resultado de evaluación |
| `GET /api/v1/assessments/results` | Listar resultados del usuario |
| `GET /api/v1/assessments/results/{id}` | Ver resultado |
| `DELETE /api/v1/assessments/results/{id}` | Eliminar resultado |

#### 🎯 Goals (objetivos)
| Endpoint | Descripción |
|---|---|
| `GET /api/v1/goals` | Listar objetivos del usuario |
| `GET /api/v1/goals/{id}` | Ver objetivo |
| `POST /api/v1/goals` | Crear objetivo |
| `PUT /api/v1/goals/{id}` | Editar objetivo |
| `DELETE /api/v1/goals/{id}` | Eliminar objetivo |
| `POST /api/v1/goals/{id}/achieve` | Marcar como conseguido |

#### 📝 Workout Logs (registro de sesiones)
| Endpoint | Descripción |
|---|---|
| `GET /api/v1/workout-logs` | Listar sesiones realizadas |
| `GET /api/v1/workout-logs/{id}` | Ver sesión |
| `POST /api/v1/workout-logs` | Registrar sesión |
| `PUT /api/v1/workout-logs/{id}` | Editar registro |
| `DELETE /api/v1/workout-logs/{id}` | Eliminar registro |

#### 🔗 Social: Follow
| Endpoint | Descripción |
|---|---|
| `POST /api/v1/users/{id}/follow` | Seguir usuario |
| `DELETE /api/v1/users/{id}/follow` | Dejar de seguir |
| `GET /api/v1/users/{id}/followers` | Lista de seguidores |
| `GET /api/v1/users/{id}/following` | Lista de seguidos |
| `GET /api/v1/users/{id}/follow-stats` | Estadísticas (siguiendo/seguidores) |

#### 📚 Catálogos (públicos)
| Endpoint | Descripción |
|---|---|
| `GET /api/v1/catalogs/exercise-categories` | Categorías de ejercicios |
| `GET /api/v1/catalogs/muscle-groups` | Grupos musculares |
| `GET /api/v1/catalogs/grip-types` | Tipos de agarre |
| `GET /api/v1/catalogs/parameter-types` | Tipos de parámetros |
| `GET /api/v1/catalogs/goal-types` | Tipos de objetivos |
| `GET /api/v1/catalogs/equipment` | Equipamiento |
| `GET /api/v1/catalogs/grades` | Grados de escalada (French + Font) |

#### 💳 Subscriptions & Billing (Stripe)
| Endpoint | Descripción |
|---|---|
| `GET /api/v1/subscriptions/tiers` | Catálogo de tiers (FREE, BASIC, PREMIUM) con sus planes |
| `GET /api/v1/subscriptions/me` | Suscripción actual del usuario autenticado |
| `POST /api/v1/billing/checkout-session` | Crear sesión de Stripe Checkout |
| `POST /api/v1/billing/customer-portal` | Crear sesión del Customer Portal (gestionar plan, facturas) |
| `GET /api/v1/billing/invoices` | Historial de facturas del usuario |
| `POST /api/v1/webhooks/stripe` | Webhook de Stripe (procesa eventos de pago — idempotente) |

---

### ❌ Pendientes de implementar

| Funcionalidad | Estado | Prioridad |
|---|---|---|
| **AI Plan Generation** — Generación asíncrona de planes con LLM externo | Schema en V11, sin controller/service/worker | 🔴 Alta |
| **Training Groups** — Grupos de entrenamiento con roles y plan compartido | Schema en V10, sin controller/service | 🟡 Media |
| **Media Management** — Assets multimedia (imágenes, vídeos) | Schema en V4, sin controller/service | 🟡 Media |
| **Social Activity Feed** — Feed de actividad de seguidos | Sin implementar | 🟢 Baja |
| **Admin Endpoints** — Gestión de contenido de plataforma (admin crea ejercicios/planes/assessments) | Sin implementar | 🟡 Media |
| **SSE para AI Jobs** — Notificaciones server-sent events para estado de jobs de IA | Sin implementar | 🟡 Media |
| **Climbing Gym / Routes** — Gimnasios y vías | Sin implementar | 🟢 Baja |

---

## 🗄️ Estructura de la BD

18 migraciones Flyway que construyen el schema completo:

| Migración | Contenido |
|---|---|
| V1 | Extensiones (`pgcrypto`) y tipos ENUM |
| V2 | `users`, `user_profiles`, subscriptions (`tiers`, `plans`, `user_subscriptions`, `invoices`, `webhook_events`, `payment_customers`) |
| V3 | Catálogos: `climbing_grades`, `exercise_categories`, `muscle_groups`, `grip_types`, `parameter_types`, `goal_types`, `equipment` (todos con traducciones) |
| V4 | `media_assets` (multimedia con storage provider abstracto) |
| V5 | `exercises` (con parámetros, músculos, traducciones) |
| V6 | `workout_templates` y `workout_template_exercises` |
| V7 | `training_plans` y su jerarquía `weeks → sessions` |
| V8 | `assessment_definitions`, `assessment_tests`, `assessment_results`, `assessment_metrics` |
| V9 | `user_goals` y `workout_logs` + `workout_log_entries` |
| V10 | `training_groups` y `user_followers` (grafo social) |
| V11 | `ai_plan_generation_jobs` (jobs asíncronos de IA) |
| V12-V15 | Seeds: assessments, contenido ES, training plans, catálogo |
| V16 | `subscription_tier_translations` (i18n de tiers) |
| V17 | `stripe_price_references` (precios de Stripe en planes) |
| V18 | Feed de plataforma (`comprehensive_platform_feed`) |

---

## 🌍 Internacionalización (i18n)

- **Locale por defecto**: `es` (español)
- **Idiomas requeridos**: ES + EN para contenido de plataforma
- El contenido creado por usuarios se almacena en el idioma en que se escribe (sin traducción automática)
- Las traducciones se resuelven vía header `Accept-Language`

---

## 📐 Convenciones de código

- **Nombrado de puertos**: `<Acción><Entidad>UseCase` (input), `<Entidad>Repository` (output)
- **Nombrado de servicios**: `<Entidad>Service`
- **Nombrado de controladores**: `<Entidad>Controller`
- **Nombrado de adaptadores JPA**: `Jpa<Entidad>Repository`
- **Inyección**: Solo por constructor (`@RequiredArgsConstructor`)
- **Validación**: Bean Validation en DTOs de entrada; validación de invariantes en constructores del dominio
- **Respuestas**: `ResponseEntity<>` con DTOs (nunca entidades de dominio directamente)

---

## 📂 Estructura del proyecto

```
src/main/java/app/onlyclimb/api/
├── OnlyClimbApiApplication.java
├── domain/
│   ├── model/          ← Entidades, value objects (50 clases)
│   ├── port/
│   │   ├── in/         ← Interfaces de casos de uso (51 interfaces)
│   │   └── out/        ← Interfaces de repositorios + puertos externos
│   └── exception/      ← Excepciones de dominio
├── application/
│   └── service/        ← Implementaciones de casos de uso
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   ├── web/    ← 13 controladores REST + DTOs
    │   │   └── auth/   ← Autenticación Clerk JWT + Svix
    │   └── out/
    │       ├── persistence/  ← 32 repositorios Spring Data JPA + mappers
    │       └── payment/      ← Stripe Payment Gateway adapter
    └── config/         ← Spring Security, OpenAPI, Stripe, Clerk, Svix
```

---

## 📄 API Docs

La documentación interactiva está disponible en:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

---

## 🔄 Flujo de usuario principal

```mermaid
flowchart TD
    A[Usuario se registra vía Clerk] --> B[Webhook crea usuario local + plan FREE]
    B --> C[Completa perfil: peso, altura, disciplina]
    C --> D[Realiza Assessment de nivel]
    D --> E[Define un Goal activo]
    E --> F{¿Premium?}
    F -->|Sí| G[Stripe Checkout → suscripción activa]
    F -->|No| H[Plan FREE]
    G --> I[AI genera TrainingPlan personalizado]
    H --> J[Elige Plan de plataforma o crea el suyo]
    I --> K[Ejecuta sesiones → WorkoutLogs]
    J --> K
    K --> L[Re-evalúa con nuevo Assessment]
    L --> E
```

---

## 🤝 Productos de referencia

- [Lattice Training](https://latticetraining.com) — Planes de entrenamiento para escalada
- [Climbro](https://climbro.com) — Evaluación y seguimiento

---

## 📝 Licencia

Propietaria. Todos los derechos reservados.
