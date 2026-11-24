package br.com.acorde.organizacoes.controller;

import br.com.acorde.organizacoes.dto.ChangeMemberRoleRequest;
import br.com.acorde.organizacoes.dto.MemberResponse;
import br.com.acorde.organizacoes.service.OrganizationMembershipService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{organizationId}/members")
public class OrganizationMembershipController {
    private final OrganizationMembershipService membershipService;

    public OrganizationMembershipController(OrganizationMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    @PreAuthorize("hasRole('GESTOR')")
    public List<MemberResponse> list(@PathVariable UUID organizationId, @AuthenticationPrincipal Jwt jwt) {
        return membershipService.listMembers(organizationId, jwt);
    }

    @PatchMapping("/{memberId}/role")
    @PreAuthorize("hasRole('GESTOR')")
    public MemberResponse changeRole(@PathVariable UUID organizationId, @PathVariable UUID memberId,
                                    @Valid @RequestBody ChangeMemberRoleRequest request,
                                    @AuthenticationPrincipal Jwt jwt) {
        return membershipService.changeRole(organizationId, memberId, request, jwt);
    }
}
