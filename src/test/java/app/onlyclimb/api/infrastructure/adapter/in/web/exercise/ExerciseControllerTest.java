package app.onlyclimb.api.infrastructure.adapter.in.web.exercise;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import app.onlyclimb.api.domain.exception.ExerciseNotFoundException;
import app.onlyclimb.api.domain.model.AuthProvider;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.Email;
import app.onlyclimb.api.domain.model.Exercise;
import app.onlyclimb.api.domain.model.SafetyWarningLevel;
import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.model.User;
import app.onlyclimb.api.domain.port.in.CreateExerciseUseCase;
import app.onlyclimb.api.domain.port.in.DeleteExerciseUseCase;
import app.onlyclimb.api.domain.port.in.GetExerciseUseCase;
import app.onlyclimb.api.domain.port.in.GetUserUseCase;
import app.onlyclimb.api.domain.port.in.ListExercisesUseCase;
import app.onlyclimb.api.domain.port.in.UpdateExerciseUseCase;
import app.onlyclimb.api.domain.port.out.ExerciseRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExerciseController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        UserAuthorization.class,
        CurrentUserService.class
})
class ExerciseControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CreateExerciseUseCase createExerciseUseCase;
    @MockitoBean UpdateExerciseUseCase updateExerciseUseCase;
    @MockitoBean DeleteExerciseUseCase deleteExerciseUseCase;
    @MockitoBean GetExerciseUseCase getExerciseUseCase;
    @MockitoBean ListExercisesUseCase listExercisesUseCase;
    @MockitoBean GetUserUseCase getUserUseCase;
    @MockitoBean UserRepository userRepository;
    @MockitoBean ClerkJwtAuthenticationConverter clerkJwtAuthenticationConverter;

    @Test
    void listIsPublic() throws Exception {
        when(listExercisesUseCase.list(any()))
                .thenReturn(new ExerciseRepository.Page<>(List.of(), null));
        mockMvc.perform(get("/api/v1/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(getExerciseUseCase.getVisible(any(), any()))
                .thenThrow(new ExerciseNotFoundException(id));
        mockMvc.perform(get("/api/v1/exercises/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void postWithoutJwtIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postWithJwtCreatesExercise() throws Exception {
        User caller = User.register(AuthProvider.CLERK, "ext-1", new Email("alice@example.com"));
        when(getUserUseCase.getByAuthIdentity(AuthProvider.CLERK, "ext-1")).thenReturn(caller);

        Exercise created = Exercise.createUserExercise(
                caller.getId(), "HANGBOARD", "FINGERS",
                DifficultyLevel.BEGINNER, SafetyWarningLevel.NONE,
                false, false, null, Set.of(),
                List.of(new Translation("en", Exercise.FIELD_NAME, "Test")),
                ContentVisibility.PRIVATE);
        when(createExerciseUseCase.create(any())).thenReturn(created);

        String body = """
            {
              "categoryCode": "HANGBOARD",
              "primaryMuscleGroupCode": "FINGERS",
              "difficultyLevel": "BEGINNER",
              "safetyWarningLevel": "NONE",
              "requiresEquipment": false,
              "isUnilateral": false,
              "translations": [{"locale":"en","field":"name","value":"Test"}]
            }
            """;

        mockMvc.perform(post("/api/v1/exercises")
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test"));
    }

    @Test
    void deleteByNonOwnerReturns403() throws Exception {
        User caller = User.register(AuthProvider.CLERK, "ext-1", new Email("alice@example.com"));
        when(getUserUseCase.getByAuthIdentity(AuthProvider.CLERK, "ext-1")).thenReturn(caller);
        doThrow(new ContentOwnershipException("not owner"))
                .when(deleteExerciseUseCase).delete(any(), any());

        mockMvc.perform(delete("/api/v1/exercises/{id}", UUID.randomUUID())
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }
}
