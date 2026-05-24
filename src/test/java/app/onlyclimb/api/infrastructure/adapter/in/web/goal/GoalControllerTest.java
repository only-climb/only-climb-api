package app.onlyclimb.api.infrastructure.adapter.in.web.goal;

import app.onlyclimb.api.domain.exception.GoalNotFoundException;
import app.onlyclimb.api.domain.exception.InvalidGradeException;
import app.onlyclimb.api.domain.model.AuthProvider;
import app.onlyclimb.api.domain.model.ClimbingGrade;
import app.onlyclimb.api.domain.model.Email;
import app.onlyclimb.api.domain.model.Goal;
import app.onlyclimb.api.domain.model.GoalType;
import app.onlyclimb.api.domain.model.GradeScale;
import app.onlyclimb.api.domain.model.User;
import app.onlyclimb.api.domain.port.in.AchieveGoalUseCase;
import app.onlyclimb.api.domain.port.in.CreateGoalUseCase;
import app.onlyclimb.api.domain.port.in.DeleteGoalUseCase;
import app.onlyclimb.api.domain.port.in.GetGoalUseCase;
import app.onlyclimb.api.domain.port.in.GetUserUseCase;
import app.onlyclimb.api.domain.port.in.ListGoalsUseCase;
import app.onlyclimb.api.domain.port.in.UpdateGoalUseCase;
import app.onlyclimb.api.domain.port.out.GoalRepository;
import app.onlyclimb.api.domain.port.out.UserRepository;
import app.onlyclimb.api.infrastructure.adapter.in.web.GlobalExceptionHandler;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.ClerkJwtAuthenticationConverter;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.CurrentUserService;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.UserAuthorization;
import app.onlyclimb.api.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        UserAuthorization.class,
        CurrentUserService.class
})
class GoalControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CreateGoalUseCase createUseCase;
    @MockitoBean UpdateGoalUseCase updateUseCase;
    @MockitoBean DeleteGoalUseCase deleteUseCase;
    @MockitoBean AchieveGoalUseCase achieveUseCase;
    @MockitoBean GetGoalUseCase getUseCase;
    @MockitoBean ListGoalsUseCase listUseCase;
    @MockitoBean GetUserUseCase getUserUseCase;
    @MockitoBean UserRepository userRepository;
    @MockitoBean ClerkJwtAuthenticationConverter clerkJwtAuthenticationConverter;

    private User authenticate() {
        User caller = User.register(AuthProvider.CLERK, "ext-1", new Email("alice@example.com"));
        when(getUserUseCase.getByAuthIdentity(AuthProvider.CLERK, "ext-1")).thenReturn(caller);
        return caller;
    }

    @Test
    void listWithoutJwtIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/goals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listReturnsData() throws Exception {
        User caller = authenticate();
        Goal g = Goal.create(caller.getId(), GoalType.FINGER_STRENGTH, null, null, "go");
        when(listUseCase.list(any()))
                .thenReturn(new GoalRepository.Page<>(List.of(g), null));

        mockMvc.perform(get("/api/v1/goals")
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("FINGER_STRENGTH"))
                .andExpect(jsonPath("$.data[0].active").value(true));
    }

    @Test
    void getCurrent_whenNone_returns404() throws Exception {
        authenticate();
        when(getUseCase.getCurrent(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/goals/current")
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCurrent_returnsGoal() throws Exception {
        User caller = authenticate();
        Goal g = Goal.create(caller.getId(), GoalType.GRADE_TARGET,
                new ClimbingGrade(GradeScale.FRENCH, "7a"), null, null);
        when(getUseCase.getCurrent(caller.getId())).thenReturn(Optional.of(g));

        mockMvc.perform(get("/api/v1/goals/current")
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("GRADE_TARGET"))
                .andExpect(jsonPath("$.targetGrade.scale").value("FRENCH"))
                .andExpect(jsonPath("$.targetGrade.value").value("7a"));
    }

    @Test
    void postCreatesGoal() throws Exception {
        User caller = authenticate();
        Goal g = Goal.create(caller.getId(), GoalType.FINGER_STRENGTH, null, null, "go");
        when(createUseCase.create(any())).thenReturn(g);

        String body = """
            {"type":"FINGER_STRENGTH","notes":"go"}
            """;

        mockMvc.perform(post("/api/v1/goals")
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("FINGER_STRENGTH"));
    }

    @Test
    void postWithUnknownGradeReturns400() throws Exception {
        authenticate();
        when(createUseCase.create(any()))
                .thenThrow(new InvalidGradeException(
                        new ClimbingGrade(GradeScale.FRENCH, "99z")));

        String body = """
            {"type":"GRADE_TARGET","targetGrade":{"scale":"FRENCH","value":"99z"}}
            """;

        mockMvc.perform(post("/api/v1/goals")
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postWithBlankTypeReturns400() throws Exception {
        authenticate();
        String body = "{}";

        mockMvc.perform(post("/api/v1/goals")
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void achieveReturnsUpdatedGoal() throws Exception {
        User caller = authenticate();
        Goal g = Goal.create(caller.getId(), GoalType.FINGER_STRENGTH, null, null, null);
        g.markAchieved(java.time.Instant.now());
        UUID id = g.getId();
        when(achieveUseCase.achieve(any(), any())).thenReturn(g);

        mockMvc.perform(post("/api/v1/goals/{id}/achieve", id)
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.achievedAt").isNotEmpty());
    }

    @Test
    void deleteReturns204() throws Exception {
        authenticate();
        mockMvc.perform(delete("/api/v1/goals/{id}", UUID.randomUUID())
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void getByIdMissingReturns404() throws Exception {
        authenticate();
        UUID id = UUID.randomUUID();
        when(getUseCase.getOwned(any(), any())).thenThrow(new GoalNotFoundException(id));

        mockMvc.perform(get("/api/v1/goals/{id}", id)
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound());
    }
}
