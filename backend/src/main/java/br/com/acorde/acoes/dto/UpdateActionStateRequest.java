package br.com.acorde.acoes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateActionStateRequest(@NotBlank @Size(max = 24) String state) {}
