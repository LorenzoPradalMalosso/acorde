package br.com.acorde.carbono.controller;

import br.com.acorde.carbono.dto.ActivityRequest;
import br.com.acorde.carbono.dto.ActivityRevisionRequest;
import br.com.acorde.carbono.dto.ActivityResponse;
import br.com.acorde.carbono.dto.CalculationResponse;
import br.com.acorde.carbono.service.CarbonActivityService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carbon/activities")
public class CarbonActivityController {
    private final CarbonActivityService activities;
    public CarbonActivityController(CarbonActivityService activities) { this.activities = activities; }

    @PostMapping("/energy")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityResponse energy(@Valid @RequestBody ActivityRequest request, @AuthenticationPrincipal Jwt jwt) {
        return activities.registerEnergy(request, jwt);
    }

    @PostMapping("/commutes")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityResponse commute(@Valid @RequestBody ActivityRequest request, @AuthenticationPrincipal Jwt jwt) {
        return activities.registerCommute(request, jwt);
    }

    @GetMapping
    public List<ActivityResponse> list(@AuthenticationPrincipal Jwt jwt) { return activities.list(jwt); }

    @GetMapping("/{activityId}")
    public ActivityResponse get(@PathVariable UUID activityId, @AuthenticationPrincipal Jwt jwt) {
        return activities.find(activityId, jwt);
    }

    @PutMapping("/{activityId}")
    public ActivityResponse revise(@PathVariable UUID activityId, @Valid @RequestBody ActivityRevisionRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return activities.revise(activityId, request, jwt);
    }

    @GetMapping("/{activityId}/calculations")
    public List<CalculationResponse> calculations(@PathVariable UUID activityId, @AuthenticationPrincipal Jwt jwt) {
        return activities.calculations(activityId, jwt);
    }

    @PostMapping("/{activityId}/reprocess")
    public ActivityResponse reprocess(@PathVariable UUID activityId, @AuthenticationPrincipal Jwt jwt) {
        return activities.reprocess(activityId, jwt);
    }
}
