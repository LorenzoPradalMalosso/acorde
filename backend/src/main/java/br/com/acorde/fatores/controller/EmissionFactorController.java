package br.com.acorde.fatores.controller;

import br.com.acorde.carbono.repository.FactorRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/factors")
public class EmissionFactorController {
    private final FactorRepository factors;
    public EmissionFactorController(FactorRepository factors) { this.factors = factors; }

    @GetMapping
    @PreAuthorize("hasAnyRole('GESTOR', 'COLABORADOR')")
    public List<Map<String, Object>> list(@RequestParam String category, @RequestParam String region,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal Jwt jwt) {
        return factors.list(category, region, date);
    }
}
