package app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import app.onlyclimb.api.domain.exception.TrainingPlanNotFoundException;
import app.onlyclimb.api.domain.model.AuthProvider;
import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.Email;
import app.onlyclimb.api.domain.model.GoalType;
import app.onlyclimb.api.domain.model.TrainingPlan;
import app.onlyclimb.api.domain.model.TrainingPlanSession;
import app.onlyclimb.api.domain.model.TrainingPlanWeek;
import app.onlyclimb.api.domain.model.TrainingVolume;
import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.model.User;
import app.onlyclimb.api.domain.port.in.CreateTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.in.DeleteTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.in.ForkTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.in.GetTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.in.GetUserUseCase;
import app.onlyclimb.api.domain.port.in.ListTrainingPlansUseCase;
import app.onlyclimb.api.domain.port.in.UpdateTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.out.TrainingPlanRepository;
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

@WebMvcTest(TrainingPlanController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        UserAuthorization.class,
        CurrentUserService.class
})
class TrainingPlanControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CreateTrainingPlanUseCase createUseCase;
    @MockitoBean UpdateTrainingPlanUseCase updateUseCase;
    @MockitoBean DeleteTrainingPlanUseCase deleteUseCase;
    @MockitoBean GetTrainingPlanUseCase getUseCase;
    @MockitoBean ListTrainingPlansUseCase listUseCase;
    @MockitoBean ForkTrainingPlanUseCase forkUseCase;
    @MockitoBean GetUserUseCase getUserUseCase;
    @MockitoBean UserRepository userRepository;
    @MockitoBean ClerkJwtAuthenticationConverter clerkJwtAuthenticationConverter;

    private static final UUID TEMPLATE_ID = UUID.randomUUID();

    private TrainingPlan samplePlan(UUID ownerId) {
        return TrainingPlan.createUserPlan(
                ownerId, ContentVisibility.PRIVATE, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH, Set.of(),
                null, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                Set.of(),
                List.of(new TrainingPlanWeek(
                        UUID.randomUUID(), 1, false,
                        List.of(new TrainingPlanSession(
                                UUID.randomUUID(), 2, 1, TEMPLATE_ID, false, List.of())),
                        List.of())),
                List.of(new Translation("es", TrainingPlan.FIELD_NAME, "Mi plan")));
    }

    @Test
    void listIsPublic() throws Exception {
        when(listUseCase.list(any()))
                .thenReturn(new TrainingPlanRepository.Page<>(List.of(), null));
        mockMvc.perform(get("/api/v1/training-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(getUseCase.get(any(), any()))
                .thenThrow(new TrainingPlanNotFoundException(id));
        mockMvc.perform(get("/api/v1/training-plans/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void postWithoutJwtIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/training-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postWithJwtCreatesPlan() throws Exception {
        User caller = User.register(AuthProvider.CLERK, "ext-1", new Email("alice@example.com"));
        when(getUserUseCase.getByAuthIdentity(AuthProvider.CLERK, "ext-1")).thenReturn(caller);
        when(createUseCase.create(any())).thenReturn(samplePlan(caller.getId()));

        String body = """
            {
              "visibility": "PRIVATE",
              "difficultyLevel": "INTERMEDIATE",
              "targetDiscipline": "SPORT",
              "primaryGoal": "FINGER_STRENGTH",
              "secondaryGoals": [],
              "durationWeeks": 4,
              "sessionsPerWeek": 3,
              "avgSessionDurationMinutes": 60,
              "trainingVolume": "MODERATE",
              "requiresHangboard": true,
              "requiresCampusBoard": false,
              "requiresGymAccess": false,
              "requiresOutdoorClimbing": false,
              "recoveryFocused": false,
              "equipment": [],
              "weeks": [
                {
                  "weekNumber": 1,
                  "deload": false,
                  "sessions": [
                    {"dayOfWeek": 2, "position": 1, "workoutTemplateId": "%s", "optional": false, "notesTranslations": []}
                  ],
                  "translations": []
                }
              ],
              "translations": [
                {"locale": "es", "field": "name", "value": "Mi plan"}
              ]
            }
            """.formatted(TEMPLATE_ID);

        mockMvc.perform(post("/api/v1/training-plans")
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mi plan"));
    }

    @Test
    void deleteByNonOwnerReturns403() throws Exception {
        User caller = User.register(AuthProvider.CLERK, "ext-1", new Email("alice@example.com"));
        when(getUserUseCase.getByAuthIdentity(AuthProvider.CLERK, "ext-1")).thenReturn(caller);
        doThrow(new ContentOwnershipException("not owner"))
                .when(deleteUseCase).delete(any(), any());

        mockMvc.perform(delete("/api/v1/training-plans/{id}", UUID.randomUUID())
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void forkRequiresJwtAndReturns201() throws Exception {
        User caller = User.register(AuthProvider.CLERK, "ext-1", new Email("alice@example.com"));
        when(getUserUseCase.getByAuthIdentity(AuthProvider.CLERK, "ext-1")).thenReturn(caller);
        when(forkUseCase.fork(any(), any())).thenReturn(samplePlan(caller.getId()));

        mockMvc.perform(post("/api/v1/training-plans/{id}/fork", UUID.randomUUID())
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
    }
}
