package br.com.acorde.organizacoes.repository;

import br.com.acorde.organizacoes.model.Organization;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Organization o where o.id = :organizationId")
    Optional<Organization> findByIdForUpdate(@Param("organizationId") UUID organizationId);
}
