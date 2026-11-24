package br.com.acorde.organizacoes.dto;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(UUID id, String name, String email, String role, Instant joinedAt) {}
