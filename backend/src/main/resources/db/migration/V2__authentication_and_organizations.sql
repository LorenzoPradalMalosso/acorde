CREATE TABLE organizacoes (
    id UUID PRIMARY KEY,
    nome VARCHAR(160) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    nome VARCHAR(160) NOT NULL,
    email VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_usuarios_email_case_insensitive ON usuarios (LOWER(email));

CREATE TABLE membros (
    id UUID PRIMARY KEY,
    organizacao_id UUID NOT NULL REFERENCES organizacoes(id),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    papel VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ingressou_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_membros_papel CHECK (papel IN ('GESTOR', 'COLABORADOR')),
    CONSTRAINT uq_membros_organizacao_usuario UNIQUE (organizacao_id, usuario_id),
    CONSTRAINT uq_membros_organizacao_id UNIQUE (organizacao_id, id)
);

CREATE TABLE sessoes_autenticacao (
    id UUID PRIMARY KEY,
    membro_id UUID NOT NULL REFERENCES membros(id),
    criado_em TIMESTAMPTZ NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    revogado_em TIMESTAMPTZ,
    CONSTRAINT ck_sessoes_expiracao CHECK (expira_em > criado_em)
);

CREATE INDEX ix_sessoes_membro ON sessoes_autenticacao(membro_id);
CREATE INDEX ix_sessoes_expiracao ON sessoes_autenticacao(expira_em);
