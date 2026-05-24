---
mode: agent
description: "Audit the codebase for hexagonal architecture violations. Use when you want to verify that the project correctly follows the dependency rule, naming conventions, and layer separation."
---

# Hexagonal Architecture Audit

Perform a thorough audit of the codebase for architecture violations. Scan all Java source files and report issues grouped by severity.

## What to Check

### 🔴 Critical — Dependency Rule Violations

These break the architecture entirely:

1. **Domain imports infrastructure or Spring**: Any file in `domain/` that imports from `org.springframework`, `jakarta.persistence`, or `infrastructure/`.
2. **Controller injects service class directly**: A `*Controller` that has a field of type `*Service` instead of a `*UseCase` interface.
3. **Application service injects Spring Data repo directly**: A `*Service` that has a field of type `JpaRepository` or `SpringData*Repository` instead of the domain `*Repository` interface.
4. **Domain model has JPA annotations**: `@Entity`, `@Table`, `@Column`, `@Id` on classes inside `domain/model/`.

### 🟡 Warning — Naming Convention Violations

1. Input port interfaces in `domain/port/in/` not suffixed with `UseCase`.
2. Output port interfaces in `domain/port/out/` not suffixed with `Repository`.
3. Application services not suffixed with `Service`.
4. Web adapters not suffixed with `Controller`.
5. JPA adapter classes not prefixed with `Jpa` or not suffixed with `Adapter`.
6. Request DTOs not suffixed with `Request`, response DTOs not suffixed with `Response`.

### 🟠 Design Issues

1. **Business logic in controllers**: Conditional logic, calculations, or state changes inside `*Controller` methods beyond mapping + delegating.
2. **Business logic in adapters**: Any logic in `*JpaAdapter` beyond mapping and repository calls.
3. **Domain entities with Lombok `@Data` or `@Setter`**: Domain models should be explicit and controlled.
4. **`@Autowired` field injection anywhere**: All injection must be via constructors.
5. **Raw domain objects returned from controllers**: Methods returning `ResponseEntity<Route>` instead of `ResponseEntity<RouteResponse>`.
6. **Missing `@Valid` on request body parameters** in controllers.

### 🔵 Info — Structural Observations

1. Files placed in wrong packages (e.g., a `*UseCase` interface not inside `domain/port/in/`).
2. Missing exception handler for domain exceptions that are thrown but not caught.

---

## Report Format

For each issue found, report:

```
[SEVERITY] File: <relative path>
Issue: <description of the violation>
Fix: <what to change>
```

After listing all issues, provide:
- **Total critical**: N
- **Total warnings**: N
- **Total design issues**: N
- **Overall assessment**: PASS / NEEDS ATTENTION / CRITICAL VIOLATIONS

If no issues are found, confirm that the architecture is clean.

## Scope

Scan only files inside `src/main/java/`. Do not audit test files in this pass.
