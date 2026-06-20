package app.onlyclimb.api.application.service;

import app.onlyclimb.api.domain.exception.DuplicateUserException;
import app.onlyclimb.api.domain.exception.UserNotFoundException;
import app.onlyclimb.api.domain.exception.UserProfileNotFoundException;
import app.onlyclimb.api.domain.model.AuthProvider;
import app.onlyclimb.api.domain.model.Email;
import app.onlyclimb.api.domain.model.Height;
import app.onlyclimb.api.domain.model.User;
import app.onlyclimb.api.domain.model.UserProfile;
import app.onlyclimb.api.domain.model.Weight;
import app.onlyclimb.api.domain.port.in.DeleteUserUseCase;
import app.onlyclimb.api.domain.port.in.GetUserProfileUseCase;
import app.onlyclimb.api.domain.port.in.GetUserUseCase;
import app.onlyclimb.api.domain.port.in.ProvisionFreeSubscriptionUseCase;
import app.onlyclimb.api.domain.port.in.RegisterUserCommand;
import app.onlyclimb.api.domain.port.in.RegisterUserUseCase;
import app.onlyclimb.api.domain.port.in.UpdateUserProfileCommand;
import app.onlyclimb.api.domain.port.in.UpdateUserProfileUseCase;
import app.onlyclimb.api.domain.port.out.UserProfileRepository;
import app.onlyclimb.api.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService
        implements RegisterUserUseCase, GetUserUseCase, DeleteUserUseCase,
                   UpdateUserProfileUseCase, GetUserProfileUseCase {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProvisionFreeSubscriptionUseCase provisionFreeSubscriptionUseCase;

    @Override
    @Transactional
    public User register(RegisterUserCommand command) {
        AuthProvider provider = command.authProvider();
        String externalId = command.externalUserId();
        Email email = new Email(command.email());

        return userRepository.findByAuthIdentity(provider, externalId)
                .map(existing -> {
                    if (!existing.getEmail().equals(email)) {
                        existing.changeEmail(email);
                    }
                    existing.recordLogin();
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    if (userRepository.existsByEmail(email)) {
                        throw new DuplicateUserException(
                                "Email already registered: " + email.value());
                    }
                    User user = User.register(provider, externalId, email);
                    User saved = userRepository.save(user);
                    userProfileRepository.save(UserProfile.empty(saved.getId()));
                    provisionFreeSubscriptionUseCase.provision(saved.getId());
                    return saved;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getByAuthIdentity(AuthProvider authProvider, String externalUserId) {
        return userRepository.findByAuthIdentity(authProvider, externalUserId)
                .orElseThrow(() -> new UserNotFoundException(externalUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getByUserId(UUID userId) {
        // Verify the user exists so we report UserNotFound instead of ProfileNotFound
        // when the user itself is missing.
        if (userRepository.findById(userId).isEmpty()) {
            throw new UserNotFoundException(userId);
        }
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));
    }

    @Override
    @Transactional
    public void deleteByAuthIdentity(AuthProvider authProvider, String externalUserId) {
        userRepository.findByAuthIdentity(authProvider, externalUserId)
                .ifPresent(user -> {
                    user.softDelete();
                    userRepository.save(user);
                });
    }

    @Override
    @Transactional
    public UserProfile updateProfile(UUID userId, UpdateUserProfileCommand command) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));

        Weight weight = command.weightKg() != null ? new Weight(command.weightKg()) : null;
        Height height = command.heightCm() != null ? new Height(command.heightCm()) : null;

        profile.update(
                command.displayName(),
                weight,
                height,
                command.primaryDiscipline(),
                command.locale()
        );
        return userProfileRepository.save(profile);
    }
}
