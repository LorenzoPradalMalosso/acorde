package br.com.acorde.auth.model;

import br.com.acorde.organizacoes.model.OrganizationMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessoes_autenticacao")
public class AuthenticationSession {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membro_id", nullable = false)
    private OrganizationMember member;

    @Column(name = "criado_em", nullable = false)
    private Instant createdAt;

    @Column(name = "expira_em", nullable = false)
    private Instant expiresAt;

    @Column(name = "revogado_em")
    private Instant revokedAt;

    protected AuthenticationSession() {}

    public AuthenticationSession(UUID id, OrganizationMember member, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.member = member;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void revoke(Instant at) { this.revokedAt = at; }
}
