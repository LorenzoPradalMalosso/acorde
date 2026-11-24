package br.com.acorde.acoes.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ActionResponse(UUID id, UUID goalId, UUID responsibleMemberId, String title,
                             String description, LocalDate dueDate, String state) {}
