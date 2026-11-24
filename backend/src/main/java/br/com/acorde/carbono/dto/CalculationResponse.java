package br.com.acorde.carbono.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CalculationResponse(
        Integer revision,
        UUID factorId,
        BigDecimal factorValue,
        String factorSource,
        String factorVersion,
        String factorScope,
        String factorRegion,
        String factorAssumptions,
        String factorActivityUnit,
        String activityUnit,
        BigDecimal activityQuantity,
        LocalDate periodStart,
        LocalDate periodEnd,
        String activityRegion,
        BigDecimal emissionsKgCo2e,
        String resultUnit,
        String state,
        boolean current,
        String justification,
        OffsetDateTime calculatedAt) {}
