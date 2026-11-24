package br.com.acorde.organizacoes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizacoes")
public class Organization {
    @Id
    private UUID id;

    @Column(name = "nome", nullable = false, length = 160)
    private String name;

    @Column(name = "criado_em", nullable = false)
    private Instant createdAt;

    protected Organization() {}

    public Organization(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
}
