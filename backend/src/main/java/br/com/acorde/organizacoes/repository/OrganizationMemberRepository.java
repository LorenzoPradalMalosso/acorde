package br.com.acorde.organizacoes.repository;

import br.com.acorde.organizacoes.model.MemberRole;
import br.com.acorde.organizacoes.model.OrganizationMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
    List<OrganizationMember> findAllByUser_IdAndActiveTrueOrderByJoinedAtAsc(UUID userId);
    List<OrganizationMember> findAllByOrganization_IdAndActiveTrueOrderByJoinedAtAsc(UUID organizationId);
    Optional<OrganizationMember> findByIdAndOrganization_IdAndActiveTrue(UUID id, UUID organizationId);
    Optional<OrganizationMember> findByUser_IdAndOrganization_IdAndActiveTrue(UUID userId, UUID organizationId);
    Optional<OrganizationMember> findByIdAndOrganization_IdAndActiveTrueAndUser_ActiveTrue(UUID id, UUID organizationId);
    long countByOrganization_IdAndRoleAndActiveTrue(UUID organizationId, MemberRole role);
}
