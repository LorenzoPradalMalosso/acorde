package br.com.acorde.auth.service;

import br.com.acorde.auth.dto.AuthProfile;
import br.com.acorde.auth.dto.LoginRequest;
import br.com.acorde.auth.dto.LoginResponse;
import br.com.acorde.auth.dto.MembershipOption;
import br.com.acorde.auth.model.AuthenticationSession;
import br.com.acorde.auth.repository.AuthenticationSessionRepository;
import br.com.acorde.organizacoes.model.OrganizationMember;
import br.com.acorde.organizacoes.model.UserAccount;
import br.com.acorde.organizacoes.repository.OrganizationMemberRepository;
import br.com.acorde.organizacoes.repository.UserAccountRepository;
import br.com.acorde.platform.errors.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private final UserAccountRepository users;
    private final OrganizationMemberRepository members;
    private final AuthenticationSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration tokenTtl;
    private final String issuer;
    private final String dummyHash;

    public AuthenticationService(UserAccountRepository users,
                                 OrganizationMemberRepository members,
                                 AuthenticationSessionRepository sessions,
                                 PasswordEncoder passwordEncoder,
                                 JwtEncoder jwtEncoder,
                                 @Value("${acorde.security.jwt.access-token-ttl:PT15M}") Duration tokenTtl,
                                 @Value("${acorde.security.jwt.issuer}") String issuer) {
        this.users = users;
        this.members = members;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.tokenTtl = tokenTtl;
        this.issuer = issuer;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        UserAccount user = users.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null) {
            if (withinBcryptLimit(request.password())) passwordEncoder.matches(request.password(), dummyHash);
            throw invalidCredentials();
        }
        if (!user.isActive() || !withinBcryptLimit(request.password())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        List<OrganizationMember> activeMemberships = members.findAllByUser_IdAndActiveTrueOrderByJoinedAtAsc(user.getId())
                .stream().filter(m -> m.getOrganization() != null).toList();
        OrganizationMember member;
        if (request.organizationId() == null) {
            if (activeMemberships.isEmpty()) throw invalidCredentials();
            if (activeMemberships.size() > 1) {
                List<MembershipOption> options = activeMemberships.stream().map(this::membershipOption).toList();
                throw new OrganizationSelectionRequired(options);
            }
            member = activeMemberships.getFirst();
        } else {
            member = members.findByUser_IdAndOrganization_IdAndActiveTrue(user.getId(), request.organizationId())
                    .orElseThrow(AuthenticationService::invalidCredentials);
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenTtl);
        UUID sessionId = UUID.randomUUID();
        sessions.save(new AuthenticationSession(sessionId, member, issuedAt, expiresAt));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(sessionId.toString())
                .claim("organization_id", member.getOrganization().getId().toString())
                .claim("member_id", member.getId().toString())
                .claim("role", member.getRole().name())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", expiresAt, profile(user, member));
    }

    @Transactional
    public void logout(Jwt jwt) {
        UUID sessionId = UUID.fromString(jwt.getId());
        sessions.findById(sessionId).ifPresent(session -> {
            if (session.getRevokedAt() == null) session.revoke(Instant.now());
        });
    }

    @Transactional(readOnly = true)
    public AuthProfile profile(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID organizationId = UUID.fromString(jwt.getClaimAsString("organization_id"));
        UUID memberId = UUID.fromString(jwt.getClaimAsString("member_id"));
        OrganizationMember member = members.findByIdAndOrganization_IdAndActiveTrue(memberId, organizationId)
                .orElseThrow(AuthenticationService::invalidCredentials);
        if (!member.getUser().getId().equals(userId)) throw invalidCredentials();
        return profile(member.getUser(), member);
    }

    private AuthProfile profile(UserAccount user, OrganizationMember member) {
        return new AuthProfile(user.getId(), user.getName(), user.getEmail(), member.getOrganization().getId(),
                member.getOrganization().getName(), member.getId(), member.getRole().name());
    }

    private MembershipOption membershipOption(OrganizationMember member) {
        return new MembershipOption(member.getOrganization().getId(), member.getOrganization().getName(),
                member.getId(), member.getRole().name());
    }

    private static boolean withinBcryptLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "E-mail ou senha inválidos");
    }
}
