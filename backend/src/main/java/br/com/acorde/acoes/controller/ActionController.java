package br.com.acorde.acoes.controller;

import br.com.acorde.acoes.dto.ActionResponse;
import br.com.acorde.acoes.dto.CreateActionRequest;
import br.com.acorde.acoes.dto.UpdateActionStateRequest;
import br.com.acorde.acoes.service.ActionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/actions")
public class ActionController {
    private final ActionService actions;

    public ActionController(ActionService actions) { this.actions = actions; }

    @GetMapping
    @PreAuthorize("hasAnyRole('GESTOR', 'COLABORADOR')")
    public List<ActionResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return actions.list(organization(jwt), member(jwt), manager(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GESTOR')")
    public ActionResponse create(@Valid @RequestBody CreateActionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return actions.create(organization(jwt), request);
    }

    @PatchMapping("/{actionId}/state")
    @PreAuthorize("hasAnyRole('GESTOR', 'COLABORADOR')")
    public ActionResponse updateState(@PathVariable UUID actionId,
                                      @Valid @RequestBody UpdateActionStateRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        return actions.updateState(actionId, request.state(), organization(jwt), member(jwt), manager(jwt));
    }

    private static UUID organization(Jwt jwt) { return UUID.fromString(jwt.getClaimAsString("organization_id")); }
    private static UUID member(Jwt jwt) { return UUID.fromString(jwt.getClaimAsString("member_id")); }
    private static boolean manager(Jwt jwt) { return "GESTOR".equals(jwt.getClaimAsString("role")); }
}
