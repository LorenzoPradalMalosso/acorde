# Acorde 2.0 — Modelo Entidade–Relacionamento (DER)

> **Versão do sistema: 2.0.** Organizações/usuários/sessões estão em V2; fatores/atividades/cálculos em V3; metas/ações em `V4__goals_and_reduction_actions.sql`. As migrations e APIs passaram testes de integração em PostgreSQL 17 temporário. D-012 e a validação acadêmica do método seguem provisórias/pendentes; não há fatores reais aprovados.
>
> **Stack preservada:** Java + Spring Boot, PostgreSQL, Web React e Mobile Flutter. O modelo reutiliza `organizacoes`, `usuarios` e `membros` do DER anterior; substitui tarefas/tags/impactos como núcleo por atividades emissoras, fatores e registros de emissão.

## 1. Escopo conceitual do MVP

O fluxo é: gestor registra energia na Web e colaborador registra deslocamento no Mobile → backend escolhe fator compatível e calcula → histórico e dashboard exibem dados parciais → gestor define metas e ações.

```mermaid
erDiagram
    ORGANIZACOES ||--o{ MEMBROS : possui
    USUARIOS ||--o{ MEMBROS : participa
    MEMBROS ||--o{ SESSOES_AUTENTICACAO : autentica
    ORGANIZACOES ||--o{ ATIVIDADES_EMISSORAS : monitora
    MEMBROS ||--o{ ATIVIDADES_EMISSORAS : registra
    ATIVIDADES_EMISSORAS ||--o| DADOS_ENERGIA : detalha
    ATIVIDADES_EMISSORAS ||--o| DADOS_DESLOCAMENTO : detalha
    ATIVIDADES_EMISSORAS ||--o{ REGISTROS_EMISSAO : calcula
    FATORES_EMISSAO ||--o{ REGISTROS_EMISSAO : utiliza
    ORGANIZACOES ||--o{ METAS_CARBONO : define
    ORGANIZACOES ||--o{ ACOES_REDUCAO : delimita
    METAS_CARBONO ||--o{ ACOES_REDUCAO : organiza
    MEMBROS ||--o{ ACOES_REDUCAO : responsavel

    ORGANIZACOES {
        uuid id PK
        varchar nome
        timestamptz criado_em
    }
    USUARIOS {
        uuid id PK
        varchar nome
        varchar email UK
        varchar senha_hash
        boolean ativo
    }
    MEMBROS {
        uuid id PK
        uuid organizacao_id FK
        uuid usuario_id FK
        varchar papel
        boolean ativo
    }
    SESSOES_AUTENTICACAO {
        uuid id PK "jti do JWT"
        uuid membro_id FK
        timestamptz criado_em
        timestamptz expira_em
        timestamptz revogado_em
    }
    ATIVIDADES_EMISSORAS {
        uuid id PK
        uuid organizacao_id FK
        uuid membro_id FK
        varchar categoria
        date periodo_inicio
        date periodo_fim
        numeric quantidade
        varchar unidade
        varchar estado
        timestamptz criado_em
    }
    DADOS_ENERGIA {
        uuid atividade_id PK,FK
        numeric consumo_kwh
        varchar regiao_rede
    }
    DADOS_DESLOCAMENTO {
        uuid atividade_id PK,FK
        varchar modalidade
        varchar regiao
        numeric distancia_ida_volta_km
        numeric dias_viagens
        numeric ocupacao
    }
    FATORES_EMISSAO {
        uuid id PK
        varchar categoria
        varchar escopo
        numeric valor
        varchar unidade_atividade
        varchar unidade_resultado
        varchar fonte
        varchar versao
        date valido_desde
        date valido_ate
        varchar regiao
        text premissas
    }
    REGISTROS_EMISSAO {
        uuid id PK
        uuid atividade_id FK
        uuid fator_id FK
        numeric quantidade_aplicada
        numeric fator_aplicado
        varchar fator_fonte_aplicada
        varchar fator_versao_aplicada
        varchar fator_escopo_aplicado
        varchar fator_regiao_aplicada
        text fator_premissas_aplicadas
        date periodo_inicio_aplicado
        date periodo_fim_aplicado
        varchar regiao_aplicada
        numeric resultado_kg_co2e
        varchar unidade_resultado
        varchar estado
        boolean vigente
        timestamptz calculado_em
    }
    METAS_CARBONO {
        uuid id PK
        uuid organizacao_id FK
        varchar categoria
        date periodo_base_inicio
        date periodo_base_fim
        numeric valor_base_kg_co2e
        numeric percentual_alvo
        date prazo
        varchar estado
        timestamptz criado_em
        timestamptz atualizado_em
    }
    ACOES_REDUCAO {
        uuid id PK
        uuid organizacao_id FK
        uuid meta_id FK
        uuid responsavel_membro_id FK
        varchar titulo
        text descricao
        date prazo
        varchar estado
        timestamptz criado_em
        timestamptz atualizado_em
    }
```

> O diagrama resume os campos executáveis de V2–V4, mas não mostra todas as constraints/índices. `DADOS_ENERGIA` e `DADOS_DESLOCAMENTO` guardam entradas tipadas; quantidade/unidade em `ATIVIDADES_EMISSORAS` são valores normalizados usados no cálculo, e cada revisão preserva seu snapshot. As migrations SQL são a autoridade física.

## 2. Entidades e cardinalidades

| Entidade | Responsabilidade e regras propostas |
|---|---|
| `organizacoes` | Escopo de isolamento de dados e papéis |
| `usuarios` | Identidade global, e-mail único e hash seguro da senha |
| `membros` | Vínculo usuário/organização e papel (`GESTOR`, `COLABORADOR`); `UNIQUE(organizacao_id, usuario_id)` |
| `atividades_emissoras` | Registro por fonte e período, pertencente a uma organização e informante; estado pode ser pendente por falta de dados/fator |
| `dados_energia` | Consumo de energia em kWh e região/rede necessária à seleção do fator |
| `dados_deslocamento` | Modalidade, região, distância de ida/volta, dias/viagens e ocupação quando pertinente; não requer GPS |
| `fatores_emissao` | Fator imutável/versionado com categoria, escopo, valor, unidade, fonte, versão, validade, região e premissas |
| `registros_emissao` | Uma revisão de cálculo; guarda fator/quantidade usados, resultado, estado e vigência |
| `metas_carbono` | Objetivo organizacional com base temporal, valor-base, redução-alvo e prazo |
| `acoes_reducao` | Trabalho de redução ligado a meta, com responsável, prazo e estado; não equivale a emissão evitada |

- Organização 1:N membros, atividades e metas; usuário 1:N membros.
- Membro 1:N sessões de autenticação; `id` da sessão corresponde ao `jti` do JWT para revogação imediata no logout.
- Membro 1:N atividades registradas e ações sob sua responsabilidade.
- Atividade 1:0..1 detalhes de energia ou deslocamento; validar coerência entre categoria e detalhe.
- Atividade 1:N revisões de cálculo; fator 1:N registros de emissão. Pode haver revisão pendente sem fator associado.
- Meta 1:N ações. O DER proposto exige meta para ação; rever se ações sem meta serão necessárias.
- Não armazenar total de dashboard como fonte independente; agregar cálculos válidos vigentes.

## 3. Integridade e metodologia

1. UUID como chaves; `TIMESTAMPTZ` para auditoria; `DATE` para períodos e prazos; `NUMERIC/DECIMAL` para quantidades, fatores e resultados.
2. Incluir `CHECK` para períodos coerentes, quantidades não negativas, reduções percentuais e domínios de estado/unidade conforme as regras aprovadas.
3. Garantir referências entre membro, atividade, fator, meta e organização. V3 aplica FK composta entre organização/membro e organização/atividade, além de validação por token no Service.
4. O fator deve preservar fonte, versão, unidade, escopo, validade, região e premissas. Uma correção publica nova versão; não altera o histórico de cálculos.
5. Cálculo: `E = Q × FE`; aceitar somente unidade/categoria/período/região compatíveis. Guardar dados efetivamente usados.
6. Registro sem fator compatível ou sem dados suficientes fica pendente e não entra no total. Ausência não é emissão zero.
7. Uma revisão válida vigente por atividade entra nas agregações; as anteriores permanecem consultáveis, sem duplicar totais. V3 aplica índice único parcial em PostgreSQL.
8. Reenvio com a mesma chave de idempotência não cria outra atividade. D-009 limita a proteção a reenvio; duplicidade intencional e caronas sem chave comum seguem pendentes.
9. Exibir emissões monitoradas com cobertura/fonte/período. Não caracterizar soma das fontes selecionadas como inventário completo.
10. Redução percentual: `(base - atual) / base × 100`, apenas se `base > 0` e os períodos forem comparáveis. Base inválida/zero significa percentual indefinido; valores negativos indicam aumento.
11. Conclusão de ação é progresso operacional e não gera automaticamente crédito, emissão evitada ou redução medida.
12. Minimizar dados pessoais; não coletar endereço ou localização precisa sem necessidade validada.

## 4. Pendências restantes após V4

- Confirmar organização piloto e fontes do MVP.
- Selecionar e validar fontes/fatores reais, regiões e premissas; não há fator real no catálogo atual.
- Confirmar academicamente o mapeamento provisório de energia a escopo 2 e deslocamento a escopo 3, incluindo método de eletricidade e compatibilidade de unidade.
- Revisar/aprovar a matriz provisória D-012 em P-004; isolamento por organização e papéis são impostos no backend.
- Confirmar regras de deduplicação de deslocamentos distintos, caronas, retenção e exclusão.
- Validar se revisão/reprocessamento apenas pelo autor é suficiente ou se outros perfis podem atuar.
- Sincronizar o diagrama estático e as notas conceituais futuras com o esquema V4; os SQL V2–V4 permanecem a referência física.

## 5. Arquivos do DER

- `DER_MVP.mmd`: fonte Mermaid do modelo 2.0.
- `DER_Completo.dbml`: modelo lógico editável, incluindo expansão.
- `futuro/DER_Expansoes.md`: entidades adiadas.
- Diagramas estáticos `.svg`/`.png` são conceituais e não refletem o esquema físico e as constraints V3; usar o SQL versionado como autoridade do esquema executável.
