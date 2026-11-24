package br.com.acorde.organizacoes.service;

import br.com.acorde.auth.repository.AuthenticationSessionRepository;
import br.com.acorde.organizacoes.dto.ChangeMemberRoleRequest;
import br.com.acorde.organizacoes.dto.MemberResponse;
import br.com.acorde.organizacoes.model.MemberRole;
import br.com.acorde.organizacoes.model.OrganizationMember;
import br.com.acorde.organizacoes.repository.OrganizationMemberRepository;
import br.com.acorde.organizacoes.repository.OrganizationRepository;
import br.com.acorde.platform.errors.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationMembershipService {
    private final OrganizationMemberRepository members;
    private final OrganizationRepository organizations;
    private final AuthenticationSessionRepository sessions;

    public OrganizationMembershipService(OrganizationMemberRepository members,
                                         OrganizationRepository organizations,
                                         AuthenticationSessionRepository sessions) {
        this.members = members;
        this.organizations = organizations;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID organizationId, Jwt actor) {
        requireOwnOrganization(organizationId, actor);
        return members.findAllByOrganization_IdAndActiveTrueOrderByJoinedAtAsc(organizationId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public MemberResponse changeRole(UUID organizationId, UUID memberId,
                                     ChangeMemberRoleRequest request, Jwt actor) {
        requireOwnOrganization(organizationId, actor);
        UUID actorMemberId = UUID.fromString(actor.getClaimAsString("member_id"));
        if (actorMemberId.equals(memberId)) {
            throw new ApiException(HttpStatus.CONFLICT, "self_role_change_not_allowed",
                    "Outro gestor deve alterar o seu próprio papel");
        }
        organizations.findByIdForUpdate(organizationId).orElseThrow(OrganizationMembershipService::notFound);
        OrganizationMember target = members.findByIdAndOrganization_IdAndActiveTrue(memberId, organizationId)
                .orElseThrow(OrganizationMembershipService::notFound);
        if (target.getRole() == MemberRole.GESTOR && request.role() == MemberRole.COLABORADOR
                && members.countByOrganization_IdAndRoleAndActiveTrue(organizationId, MemberRole.GESTOR) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "last_manager_required",
                    "A organização deve manter pelo menos um gestor ativo");
        }
        target.changeRole(request.role());
        sessions.revokeAllForMember(target.getId(), Instant.now());
        return toResponse(target);
    }

    private void requireOwnOrganization(UUID requestedOrganizationId, Jwt actor) {
        UUID authenticatedOrganization = UUID.fromString(actor.getClaimAsString("organization_id"));
        if (!authenticatedOrganization.equals(requestedOrganizationId)) throw notFound();
    }

    private MemberResponse toResponse(OrganizationMember member) {
        return new MemberResponse(member.getId(), member.getUser().getName(), member.getUser().getEmail(),
                member.getRole().name(), member.getJoinedAt());
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "resource_not_found", "Recurso não encontrado");
    }
}
