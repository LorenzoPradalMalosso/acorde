package br.com.acorde.metas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateGoalStateRequest(@NotBlank @Size(max = 20) String state) {}
