package br.com.acorde.auth.dto;

import java.util.UUID;

public record AuthProfile(UUID userId, String name, String email,
                          UUID organizationId, String organizationName,
                          UUID memberId, String role) {}
