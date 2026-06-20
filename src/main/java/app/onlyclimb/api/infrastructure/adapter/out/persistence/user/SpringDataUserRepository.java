package app.onlyclimb.api.infrastructure.adapter.out.persistence.user;

import app.onlyclimb.api.domain.model.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByUuid(UUID uuid);

    Optional<UserJpaEntity> findByAuthProviderAndExternalUserId(
            AuthProvider authProvider, String externalUserId);

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByAuthProviderAndExternalUserId(
            AuthProvider authProvider, String externalUserId);

    boolean existsByEmail(String email);
}
