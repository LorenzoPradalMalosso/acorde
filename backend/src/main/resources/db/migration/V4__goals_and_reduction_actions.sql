CREATE TABLE metas_carbono (
    id UUID PRIMARY KEY,
    organizacao_id UUID NOT NULL REFERENCES organizacoes(id),
    categoria VARCHAR(40) NOT NULL CHECK (categoria IN ('ENERGIA_ELETRICA', 'DESLOCAMENTO')),
    periodo_base_inicio DATE NOT NULL,
    periodo_base_fim DATE NOT NULL,
    valor_base_kg_co2e NUMERIC(20,8) NOT NULL CHECK (valor_base_kg_co2e >= 0),
    percentual_alvo NUMERIC(5,2) NOT NULL CHECK (percentual_alvo > 0 AND percentual_alvo <= 100),
    prazo DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ATIVA' CHECK (estado IN ('ATIVA', 'CONCLUIDA', 'CANCELADA')),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_meta_periodo_base CHECK (periodo_base_fim >= periodo_base_inicio),
    CONSTRAINT uq_meta_organizacao_id UNIQUE (organizacao_id, id)
);

CREATE TABLE acoes_reducao (
    id UUID PRIMARY KEY,
    organizacao_id UUID NOT NULL REFERENCES organizacoes(id),
    meta_id UUID NOT NULL,
    responsavel_membro_id UUID NOT NULL,
    titulo VARCHAR(160) NOT NULL,
    descricao TEXT,
    prazo DATE NOT NULL,
    estado VARCHAR(24) NOT NULL DEFAULT 'PLANEJADA'
        CHECK (estado IN ('PLANEJADA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA')),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_acao_organizacao_id UNIQUE (organizacao_id, id),
    CONSTRAINT fk_acao_meta_org FOREIGN KEY (organizacao_id, meta_id)
        REFERENCES metas_carbono(organizacao_id, id),
    CONSTRAINT fk_acao_responsavel_org FOREIGN KEY (organizacao_id, responsavel_membro_id)
        REFERENCES membros(organizacao_id, id)
);

CREATE INDEX ix_metas_org_prazo ON metas_carbono (organizacao_id, prazo);
CREATE INDEX ix_acoes_org_responsavel_estado ON acoes_reducao (organizacao_id, responsavel_membro_id, estado);
