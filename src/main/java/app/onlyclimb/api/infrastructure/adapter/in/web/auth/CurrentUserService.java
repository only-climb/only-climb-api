package app.onlyclimb.api.infrastructure.adapter.in.web.auth;

import app.onlyclimb.api.domain.exception.UserNotFoundException;
import app.onlyclimb.api.domain.model.AuthProvider;
import app.onlyclimb.api.domain.model.User;
import app.onlyclimb.api.domain.port.in.GetUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the authenticated principal to a local {@link User} aggregate.
 * Used by {@code /me} endpoints.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final GetUserUseCase getUserUseCase;

    public User requireCurrent(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new UserNotFoundException("anonymous");
        }
        String externalId = jwt.getToken().getSubject();
        if (externalId == null || externalId.isBlank()) {
            throw new UserNotFoundException("anonymous");
        }
        return getUserUseCase.getByAuthIdentity(AuthProvider.CLERK, externalId);
    }

    /** Best-effort resolution: returns empty for anonymous or unknown principals. */
    public Optional<UUID> optionalCurrentId(Authentication authentication) {
        try {
            return Optional.of(requireCurrent(authentication).getId());
        } catch (UserNotFoundException ex) {
            return Optional.empty();
        }
    }
}
