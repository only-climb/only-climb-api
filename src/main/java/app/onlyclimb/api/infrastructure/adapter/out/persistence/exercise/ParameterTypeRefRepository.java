package app.onlyclimb.api.infrastructure.adapter.out.persistence.exercise;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ParameterTypeRefRepository extends JpaRepository<ParameterTypeRef, Long> {
    Optional<ParameterTypeRef> findByCode(String code);
    List<ParameterTypeRef> findByIdIn(List<Long> ids);
    List<ParameterTypeRef> findByCodeIn(List<String> codes);
}
