package br.com.acorde.organizacoes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "membros")
public class OrganizationMember {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 20)
    private MemberRole role;

    @Column(name = "ativo", nullable = false)
    private boolean active;

    @Column(name = "ingressou_em", nullable = false)
    private Instant joinedAt;

    protected OrganizationMember() {}

    public OrganizationMember(UUID id, Organization organization, UserAccount user, MemberRole role, boolean active) {
        this.id = id;
        this.organization = organization;
        this.user = user;
        this.role = role;
        this.active = active;
        this.joinedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Organization getOrganization() { return organization; }
    public UserAccount getUser() { return user; }
    public MemberRole getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getJoinedAt() { return joinedAt; }

    public void changeRole(MemberRole role) { this.role = role; }
}
