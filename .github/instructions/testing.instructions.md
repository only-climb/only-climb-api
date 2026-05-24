---
description: "Use when writing or modifying tests for any layer. Defines the correct test strategy per layer in a hexagonal architecture: unit tests for domain, mocked unit tests for application services, slice tests for controllers and JPA adapters."
applyTo: "src/test/**"
---

# Testing Strategy by Layer

Each layer has a **specific test type**. Never use `@SpringBootTest` unless you are writing a true end-to-end integration test — it loads the full context and defeats the purpose of layered architecture.

## Layer → Test Type Quick Reference

| Layer | Test type | Spring context? | Key annotations |
|---|---|---|---|
| Domain entity / Value Object | Pure unit test | ❌ No | — |
| Application service | Unit test with mocks | ❌ No | `@ExtendWith(MockitoExtension.class)` |
| REST controller | Web slice test | Partial ✅ | `@WebMvcTest`, `@MockitoBean` |
| JPA adapter | Persistence slice test | Partial ✅ | `@DataJpaTest` |
| Full flow | Integration test | ✅ Full | `@SpringBootTest` + `@AutoConfigureMockMvc` |

---

## 1. Domain Layer — Pure Unit Tests

No Spring, no Mockito. Just JUnit 5.

Test invariants, behavior methods, and value object constraints.

```java
class RouteTest {

    @Test
    void shouldCreateRouteWithValidGrade() {
        var route = new Route(new RouteGrade("6a"));
        assertThat(route.getStatus()).isEqualTo(RouteStatus.DRAFT);
    }

    @Test
    void shouldThrowWhenPublishingAlreadyPublishedRoute() {
        var route = new Route(new RouteGrade("6a"));
        route.publish();
        assertThatThrownBy(route::publish)
                .isInstanceOf(RouteAlreadyPublishedException.class);
    }

    @Test
    void shouldRejectBlankGrade() {
        assertThatThrownBy(() -> new RouteGrade(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

---

## 2. Application Service — Unit Tests with Mockito

Mock all output ports. Never instantiate JPA repositories or real databases.

```java
@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    RouteRepository routeRepository; // mock the output port interface

    @InjectMocks
    RouteService routeService;

    @Test
    void shouldCreateAndSaveRoute() {
        var command = new CreateRouteCommand("6a", 1L);
        var expectedRoute = new Route(new RouteGrade("6a"));
        given(routeRepository.save(any())).willReturn(expectedRoute);

        var result = routeService.createRoute(command);

        assertThat(result.getGrade().getValue()).isEqualTo("6a");
        then(routeRepository).should().save(any(Route.class));
    }

    @Test
    void shouldThrowWhenRouteNotFound() {
        given(routeRepository.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> routeService.findRoute(99L))
                .isInstanceOf(RouteNotFoundException.class);
    }
}
```

---

## 3. REST Controller — Web Slice Test

Use `@WebMvcTest` to load only the web layer. Mock use cases with `@MockitoBean`.

- Do NOT mock the service class — mock the **use case interface** it implements.
- Use `MockMvc` to make HTTP calls and assert status codes + JSON bodies.
- Test validation errors (400) alongside happy paths.

```java
@WebMvcTest(RouteController.class)
class RouteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean CreateRouteUseCase createRouteUseCase; // mock the interface
    @MockitoBean FindRouteUseCase findRouteUseCase;

    @Test
    void shouldReturn201WhenCreatingValidRoute() throws Exception {
        var request = new CreateRouteRequest("6a", 1L);
        var route = new Route(new RouteGrade("6a"));
        given(createRouteUseCase.createRoute(any())).willReturn(route);

        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grade").value("6a"));
    }

    @Test
    void shouldReturn400WhenGradeIsBlank() throws Exception {
        var request = new CreateRouteRequest("", 1L);
        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
```

---

## 4. JPA Adapter — Persistence Slice Test

Use `@DataJpaTest` to load only the persistence layer (in-memory H2 or Testcontainers).

Test the adapter class, not the Spring Data repository directly.

```java
@DataJpaTest
class RouteJpaAdapterTest {

    @Autowired SpringDataRouteRepository springRepo;

    RouteJpaAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RouteJpaAdapter(springRepo);
    }

    @Test
    void shouldSaveAndRetrieveRoute() {
        var route = new Route(new RouteGrade("7b"));
        var saved = adapter.save(route);

        var found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getGrade().getValue()).isEqualTo("7b");
    }
}
```

---

## Naming Convention

| What | Pattern | Example |
|---|---|---|
| Domain entity test | `{Entity}Test` | `RouteTest` |
| Value object test | `{ValueObject}Test` | `RouteGradeTest` |
| Application service test | `{Entity}ServiceTest` | `RouteServiceTest` |
| Controller test | `{Entity}ControllerTest` | `RouteControllerTest` |
| JPA adapter test | `{Entity}JpaAdapterTest` | `RouteJpaAdapterTest` |

## General Rules

- Use **AssertJ** (`assertThat`, `assertThatThrownBy`) over JUnit assertions — more readable.
- Use **BDDMockito** (`given/then`) over `when/verify` for consistency.
- One test class per production class.
- Test file packages must mirror source packages.
- Never test private methods — test observable behavior only.
