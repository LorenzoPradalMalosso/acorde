package br.com.acorde.acoes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateActionRequest(
        @NotNull UUID goalId,
        @NotNull UUID responsibleMemberId,
        @NotBlank @Size(max = 160) String title,
        @Size(max = 4000) String description,
        @NotNull LocalDate dueDate) {}
