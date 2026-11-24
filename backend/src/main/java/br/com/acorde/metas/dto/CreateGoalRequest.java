package br.com.acorde.metas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGoalRequest(
        @NotBlank String category,
        @NotNull LocalDate basePeriodStart,
        @NotNull LocalDate basePeriodEnd,
        @NotNull @DecimalMin("0") BigDecimal baseValueKgCo2e,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal targetPercent,
        @NotNull LocalDate dueDate) {}
