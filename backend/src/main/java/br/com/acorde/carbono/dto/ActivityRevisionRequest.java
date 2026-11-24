package br.com.acorde.carbono.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ActivityRevisionRequest(
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @NotBlank @Size(max = 160) String region,
        @DecimalMin(value = "0.000001") BigDecimal consumptionKwh,
        @Size(max = 40) String mode,
        @DecimalMin(value = "0.000001") BigDecimal roundTripDistanceKm,
        @DecimalMin(value = "0.0001") BigDecimal commutingDays,
        @DecimalMin(value = "1") BigDecimal occupancy) {}
