package br.com.acorde.carbono.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ActivityResponse(UUID id, String category, LocalDate periodStart, LocalDate periodEnd,
                               BigDecimal quantity, String unit, String state, String justification,
                               Integer revision, BigDecimal emissionsKgCo2e, boolean current, boolean canReprocess) {}
