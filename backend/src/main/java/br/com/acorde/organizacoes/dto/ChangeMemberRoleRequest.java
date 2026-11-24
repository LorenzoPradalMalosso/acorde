package br.com.acorde.organizacoes.dto;

import br.com.acorde.organizacoes.model.MemberRole;
import jakarta.validation.constraints.NotNull;

public record ChangeMemberRoleRequest(@NotNull MemberRole role) {}
