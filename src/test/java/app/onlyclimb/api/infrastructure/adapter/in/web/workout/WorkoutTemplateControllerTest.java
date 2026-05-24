package app.onlyclimb.api.infrastructure.adapter.in.web.workout;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import app.onlyclimb.api.domain.exception.InvalidExerciseConfigException;
import app.onlyclimb.api.domain.exception.WorkoutTemplateNotFoundException;
import app.onlyclimb.api.domain.model.AuthProvider;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.Email;
import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.model.User;
import app.onlyclimb.api.domain.model.WorkoutTemplate;
import app.onlyclimb.api.domain.model.WorkoutTemplateExercise;
import app.onlyclimb.api.domain.port.in.CreateWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.DeleteWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.ForkWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.GetUserUseCase;
import app.onlyclimb.api.domain.port.in.GetWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.ListWorkoutTemplatesUseCase;
import app.onlyclimb.api.domain.port.in.UpdateWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.out.UserRepository;
import app.onlyclimb.api.domain.port.out.WorkoutTemplateRepository;
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
import java.util.Map;
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

@WebMvcTest(WorkoutTemplateController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        UserAuthorization.class,
        CurrentUserService.class
})
class WorkoutTemplateControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CreateWorkoutTemplateUseCase createUseCase;
    @MockitoBean UpdateWorkoutTemplateUseCase updateUseCase;
    @MockitoBean DeleteWorkoutTemplateUseCase deleteUseCase;
    @MockitoBean GetWorkoutTemplateUseCase getUseCase;
    @MockitoBean ListWorkoutTemplatesUseCase listUseCase;
    @MockitoBean ForkWorkoutTemplateUseCase forkUseCase;
    @MockitoBean GetUserUseCase getUserUseCase;
    @MockitoBean UserRepository userRepository;
    @MockitoBean ClerkJwtAuthenticationConverter clerkJwtAuthenticationConverter;

    private static final UUID EXERCISE_ID = UUID.randomUUID();

    private WorkoutTemplate sampleTemplate(UUID ownerId) {
        return WorkoutTemplate.createUserTemplate(
                ownerId, ContentVisibility.PRIVATE, DifficultyLevel.BEGINNER, null, null,
                List.of(new WorkoutTemplateExercise(1, EXERCISE_ID, Map.of(), List.of())),
                List.of(new Translation("en", WorkoutTemplate.FIELD_NAME, "Quick session")));
    }

    @Test
    void listIsPublic() throws Exception {
        when(listUseCase.list(any()))
                .thenReturn(new WorkoutTemplateRepository.Page<>(List.of(), null));
        mockMvc.perform(get("/api/v1/workout-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(getUseCase.getVisible(any(), any()))
                .thenThrow(new WorkoutTemplateNotFoundException(id));
        mockMvc.perform(get("/api/v1/workout-templates/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void postWithoutJwtIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/workout-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postWithJwtCreatesTemplate() throws Exception {
        User caller = User.register(AuthProvider.CLERK, "ext-1", new Email("alice@example.com"));
        when(getUserUseCase.getByAuthIdentity(AuthProvider.CLERK, "ext-1")).thenReturn(caller);
        when(createUseCase.create(any())).thenReturn(sampleTemplate(caller.getId()));

        String body = """
            {
              "difficultyLevel": "BEGINNER",
              "visibility": "PRIVATE",
              "exercises": [
                {"position":1,"exerciseId":"%s","config":{},"notesTranslations":[]}
              ],
              "translations": [
                {"locale":"en","field":"name","value":"Quick session"}
              ]
            }
            """.formatted(EXERCISE_ID);

        mockMvc.perform(post("/api/v1/workout-templates")
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Quick session"));
    }

    @Test
    void postWithInvalidConfigReturns400() throws Exception {
        User caller = User.register(AuthProvider.CLERK, "ext-1", new Email("alice@example.com"));
        when(getUserUseCase.getByAuthIdentity(AuthProvider.CLERK, "ext-1")).thenReturn(caller);
        when(createUseCase.create(any()))
                .thenThrow(new InvalidExerciseConfigException("Unknown config keys: [WEIGHT_KG]"));

        String body = """
            {
              "difficultyLevel": "BEGINNER",
              "visibility": "PRIVATE",
              "exercises": [
                {"position":1,"exerciseId":"%s","config":{"WEIGHT_KG":"20"},"notesTranslations":[]}
              ],
              "translations": [
                {"locale":"en","field":"name","value":"X"}
              ]
            }
            """.formatted(EXERCISE_ID);

        mockMvc.perform(post("/api/v1/workout-templates")
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteByNonOwnerReturns403() throws Exception {
        User caller = User.register(AuthProvider.CLERK, "ext-1", new Email("alice@example.com"));
        when(getUserUseCase.getByAuthIdentity(AuthProvider.CLERK, "ext-1")).thenReturn(caller);
        doThrow(new ContentOwnershipException("not owner"))
                .when(deleteUseCase).delete(any(), any());

        mockMvc.perform(delete("/api/v1/workout-templates/{id}", UUID.randomUUID())
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void forkRequiresJwt_andReturns201() throws Exception {
        User caller = User.register(AuthProvider.CLERK, "ext-1", new Email("alice@example.com"));
        when(getUserUseCase.getByAuthIdentity(AuthProvider.CLERK, "ext-1")).thenReturn(caller);
        when(forkUseCase.fork(any(), any())).thenReturn(sampleTemplate(caller.getId()));

        mockMvc.perform(post("/api/v1/workout-templates/{id}/fork", UUID.randomUUID())
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
    }
}
