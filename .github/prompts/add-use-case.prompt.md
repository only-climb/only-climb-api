---
mode: agent
description: "Add a new use case (operation) to an existing domain entity. Use this when the entity and its base structure already exist and you only need to add a new operation such as update, delete, list, or a custom business action."
---

# Add Use Case to Existing Entity

You are adding a **new use case** to an entity that already exists in the codebase. Do not regenerate existing files — only create or modify what is strictly necessary.

## Input Required

Ask the user for:
1. **Entity name** (e.g., `Route`) — must already exist in `domain/model/`
2. **Use case name** (e.g., `UpdateRoute`, `DeleteRoute`, `PublishRoute`)
3. **What the operation does** — brief description of the business logic
4. **Input data needed** — which fields/parameters the operation requires
5. **What it returns** — domain entity, void, boolean, etc.

## Files to Create or Modify

### Always CREATE (new files)

**`domain/port/in/{Action}{Entity}UseCase.java`**
- Plain Java interface
- Single method with a descriptive name

**`domain/port/in/{Action}{Entity}Command.java`** (only if the use case takes structured input)
- Java record with the needed fields

### Always MODIFY (add to existing files)

**`application/service/{Entity}Service.java`**
- Add `implements {Action}{Entity}UseCase` to the class declaration
- Implement the new method — delegate logic to the domain entity, use output port to persist

**`infrastructure/adapter/in/web/{Entity}Controller.java`**
- Inject the new use case interface via constructor (`@MockitoBean` in the field list via `@RequiredArgsConstructor`)
- Add the new endpoint method with appropriate HTTP verb:
  - Create → `@PostMapping`
  - Update → `@PutMapping("/{id}")` or `@PatchMapping("/{id}")`
  - Delete → `@DeleteMapping("/{id}")`
  - Custom query → `@GetMapping`

**`infrastructure/adapter/in/web/GlobalExceptionHandler.java`**
- Add handler for any new domain exception introduced by this use case

### Create only if the operation adds new persistence needs

**New query method in `infrastructure/adapter/out/persistence/SpringData{Entity}Repository.java`** (Spring Data derived query or `@Query`)

**New method in `infrastructure/adapter/out/persistence/{Entity}JpaAdapter.java`** implementing the new output port method

**New method in `domain/port/out/{Entity}Repository.java`** if the operation requires a new persistence query

### Create only if the operation introduces new DTOs

**`infrastructure/adapter/in/web/dto/{Action}{Entity}Request.java`** (record with Bean Validation)

**`infrastructure/adapter/in/web/dto/{Entity}Response.java`** — update if the response shape changes, or reuse existing

## Checklist Before Finishing

- [ ] New use case is an interface in `domain/port/in/` — not a class
- [ ] Application service implements the interface (added to `implements` list)
- [ ] Controller injects the use case **interface**, not the service class
- [ ] No business logic was added to the controller or service — it delegates to the domain entity
- [ ] If a new domain exception was created, it is mapped in `GlobalExceptionHandler`
- [ ] No existing files were unnecessarily regenerated or overwritten
