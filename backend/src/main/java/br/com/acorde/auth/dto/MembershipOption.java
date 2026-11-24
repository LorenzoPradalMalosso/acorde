package br.com.acorde.auth.dto;

import java.util.UUID;

public record MembershipOption(UUID organizationId, String organizationName, UUID memberId, String role) {}
