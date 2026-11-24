CREATE TABLE fatores_emissao (
    id UUID PRIMARY KEY,
    categoria VARCHAR(40) NOT NULL,
    escopo VARCHAR(40) NOT NULL,
    valor NUMERIC(20,10) NOT NULL CHECK (valor > 0),
    unidade_atividade VARCHAR(60) NOT NULL,
    unidade_resultado VARCHAR(40) NOT NULL,
    fonte VARCHAR(500) NOT NULL,
    versao VARCHAR(100) NOT NULL,
    valido_desde DATE NOT NULL,
    valido_ate DATE,
    regiao VARCHAR(160) NOT NULL,
    premissas TEXT NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (valido_ate IS NULL OR valido_ate >= valido_desde),
    UNIQUE (categoria, escopo, unidade_atividade, unidade_resultado, fonte, versao, regiao)
);

CREATE TABLE atividades_emissoras (
    id UUID PRIMARY KEY,
    organizacao_id UUID NOT NULL REFERENCES organizacoes(id),
    membro_id UUID NOT NULL,
    categoria VARCHAR(40) NOT NULL CHECK (categoria IN ('ENERGIA_ELETRICA', 'DESLOCAMENTO')),
    periodo_inicio DATE NOT NULL,
    periodo_fim DATE NOT NULL,
    quantidade NUMERIC(20,6) NOT NULL CHECK (quantidade > 0),
    unidade VARCHAR(60) NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDENTE', 'VALIDA', 'SUBSTITUIDA')),
    justificativa_pendencia VARCHAR(500),
    chave_idempotencia UUID NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_atividade_periodo CHECK (periodo_fim >= periodo_inicio),
    CONSTRAINT uq_atividade_org_membro_id UNIQUE (organizacao_id, membro_id, id),
    CONSTRAINT uq_atividade_org_id UNIQUE (organizacao_id, id),
    CONSTRAINT uq_atividade_idempotencia UNIQUE (organizacao_id, membro_id, chave_idempotencia),
    CONSTRAINT fk_atividade_membro_org FOREIGN KEY (organizacao_id, membro_id)
        REFERENCES membros(organizacao_id, id)
);

CREATE TABLE dados_energia (
    atividade_id UUID PRIMARY KEY,
    consumo_kwh NUMERIC(20,6) NOT NULL CHECK (consumo_kwh > 0),
    regiao_rede VARCHAR(160) NOT NULL,
    FOREIGN KEY (atividade_id) REFERENCES atividades_emissoras(id) ON DELETE CASCADE
);

CREATE TABLE dados_deslocamento (
    atividade_id UUID PRIMARY KEY,
    modalidade VARCHAR(40) NOT NULL,
    regiao VARCHAR(160) NOT NULL,
    distancia_ida_volta_km NUMERIC(20,6) NOT NULL CHECK (distancia_ida_volta_km > 0),
    dias_viagens NUMERIC(12,4) NOT NULL CHECK (dias_viagens > 0),
    ocupacao NUMERIC(12,4),
    FOREIGN KEY (atividade_id) REFERENCES atividades_emissoras(id) ON DELETE CASCADE,
    CHECK (ocupacao IS NULL OR ocupacao >= 1)
);

CREATE TABLE registros_emissao (
    id UUID PRIMARY KEY,
    atividade_id UUID NOT NULL,
    organizacao_id UUID NOT NULL,
    revisao INTEGER NOT NULL CHECK (revisao > 0),
    fator_id UUID REFERENCES fatores_emissao(id),
    fator_valor_aplicado NUMERIC(20,10),
    fator_fonte_aplicada VARCHAR(500),
    fator_versao_aplicada VARCHAR(100),
    fator_escopo_aplicado VARCHAR(40),
    fator_regiao_aplicada VARCHAR(160),
    fator_premissas_aplicadas TEXT,
    fator_unidade_aplicada VARCHAR(60),
    unidade_atividade_aplicada VARCHAR(60) NOT NULL,
    quantidade_aplicada NUMERIC(20,6) NOT NULL CHECK (quantidade_aplicada > 0),
    periodo_inicio_aplicado DATE NOT NULL,
    periodo_fim_aplicado DATE NOT NULL,
    regiao_aplicada VARCHAR(160) NOT NULL,
    resultado_kg_co2e NUMERIC(32,8),
    unidade_resultado VARCHAR(40),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDENTE', 'VALIDA', 'SUBSTITUIDA')),
    vigente BOOLEAN NOT NULL,
    justificativa TEXT,
    calculado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_registro_atividade_org FOREIGN KEY (organizacao_id, atividade_id)
        REFERENCES atividades_emissoras(organizacao_id, id),
    CONSTRAINT uq_registro_revisao UNIQUE (atividade_id, revisao),
    CONSTRAINT ck_registro_periodo_aplicado CHECK (periodo_fim_aplicado >= periodo_inicio_aplicado),
    CHECK (estado <> 'VALIDA' OR (fator_fonte_aplicada IS NOT NULL AND fator_versao_aplicada IS NOT NULL
        AND fator_escopo_aplicado IS NOT NULL AND fator_regiao_aplicada IS NOT NULL
        AND fator_premissas_aplicadas IS NOT NULL AND fator_unidade_aplicada IS NOT NULL)),
    CHECK ((estado = 'VALIDA' AND fator_id IS NOT NULL AND fator_valor_aplicado > 0
        AND resultado_kg_co2e >= 0 AND unidade_resultado IS NOT NULL)
        OR estado = 'SUBSTITUIDA'
        OR (estado = 'PENDENTE' AND resultado_kg_co2e IS NULL)),
    CHECK (NOT vigente OR estado = 'VALIDA')
);

CREATE UNIQUE INDEX uq_registro_vigente_por_atividade
    ON registros_emissao (atividade_id) WHERE vigente = TRUE;
CREATE INDEX ix_atividades_org_periodo ON atividades_emissoras (organizacao_id, periodo_inicio, periodo_fim);
CREATE INDEX ix_registros_org_vigentes ON registros_emissao (organizacao_id, calculado_em) WHERE vigente = TRUE;
