package br.com.acorde.metas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(UUID id, String category, LocalDate basePeriodStart, LocalDate basePeriodEnd,
                           BigDecimal baseValueKgCo2e, BigDecimal targetPercent, LocalDate dueDate,
                           String state) {}
