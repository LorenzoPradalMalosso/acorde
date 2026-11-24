package br.com.acorde.dashboard.controller;

import br.com.acorde.dashboard.dto.DashboardSummary;
import br.com.acorde.dashboard.service.DashboardService;
import br.com.acorde.platform.errors.ApiException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping
    @PreAuthorize("hasRole('GESTOR')")
    public DashboardSummary summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal Jwt jwt) {
        if ((from == null) != (to == null) || (from != null && from.isAfter(to))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_error", "Informe um intervalo completo e válido.");
        }
        return dashboard.summary(UUID.fromString(jwt.getClaimAsString("organization_id")), from, to);
    }
}
