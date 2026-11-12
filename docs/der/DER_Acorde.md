# Acorde — Diagrama Entidade–Relacionamento (DER)

> **Status:** proposta de modelo lógico, baseada nos requisitos do TCC. **Não é um banco implementado nem um modelo validado pela equipe.** Atualizar após aprovação do escopo e revisão das regras de negócio.
>
> **Stack:** Java + Spring Boot, PostgreSQL, Web React e Mobile Flutter. **Entrega acadêmica:** MVP em seis semanas, com funcionalidades complementares planejadas separadamente.

## 1. DER do MVP (7 entidades)

O núcleo viabiliza o fluxo principal: **gestor cria tarefa na Web → colaborador consulta e conclui no Mobile → sistema registra impacto ambiental estimado → dashboard Web apresenta resultados**.

```mermaid
erDiagram
    ORGANIZACOES ||--o{ MEMBROS : possui
    USUARIOS ||--o{ MEMBROS : participa
    ORGANIZACOES ||--o{ TAREFAS : contem
    MEMBROS ||--o{ TAREFAS : cria
    MEMBROS o|--o{ TAREFAS : responsavel
    TAREFAS ||--o{ TAREFAS_TAGS : recebe
    TAGS_VERDES ||--o{ TAREFAS_TAGS : classifica
    TAREFAS_TAGS ||--o| REGISTROS_IMPACTO : produz

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
        decimal capacidade_semanal_horas
    }
    TAREFAS {
        uuid id PK
        uuid organizacao_id FK
        uuid criado_por_membro_id FK
        uuid responsavel_membro_id FK
        varchar titulo
        varchar status
        varchar prioridade
        date data_limite
        decimal horas_estimadas
        timestamptz concluida_em
    }
    TAGS_VERDES {
        uuid id PK
        varchar codigo UK
        varchar nome
        varchar tipo_indicador
        boolean ativa
    }
    TAREFAS_TAGS {
        uuid id PK
        uuid tarefa_id FK
        uuid tag_verde_id FK
        timestamptz associada_em
    }
    REGISTROS_IMPACTO {
        uuid id PK
        uuid tarefa_tag_id FK
        decimal quantidade_base
        varchar unidade_base
        decimal fator_conversao
        decimal valor_estimado
        varchar unidade_resultado
        text fonte_fator
        timestamptz calculado_em
    }
```

**Arquivos:** `DER_MVP.svg` (vetorial), `DER_MVP.png` (imagem) e `DER_MVP.mmd` (código editável).

### Entidades do MVP

| Entidade | Para que serve | Observações |
|---|---|---|
| `organizacoes` | Agrupa os dados de uma empresa/equipe. | **Decisão proposta:** mesmo que o piloto use uma organização, preparar isolamento futuro. |
| `usuarios` | Identidades para login e autenticação. | `email` único; senha somente como hash seguro. |
| `membros` | Vínculo de um usuário à organização, com perfil. | `GESTOR` ou `COLABORADOR`; `UNIQUE(organizacao_id, usuario_id)`. |
| `tarefas` | Atividades com responsável, prazo, status e horas estimadas. | Responsável opcional; criador obrigatório; checar pertencimento à organização. |
| `tags_verdes` | Catálogo de ações sustentáveis. | Ex.: `DIGITALIZACAO`; não implica comprovação de impacto. |
| `tarefas_tags` | Relação N:N entre tarefas e tags. | `UNIQUE(tarefa_id, tag_verde_id)`. |
| `registros_impacto` | Guarda **estimativas**, unidades, premissas e fator usado. | No MVP cada vínculo tarefa/tag admite 0 ou 1 estimativa. |

### Cardinalidades importantes

- `organizacoes` **1:N** `membros`; `usuarios` **1:N** `membros` — um usuário pode pertencer a mais de uma organização, mas um vínculo de membro pertence a uma única organização.
- `organizacoes` **1:N** `tarefas` — toda tarefa pertence a uma organização.
- `membros` **1:N** `tarefas` como **criador**; **0:N** como **responsável** — uma tarefa pode começar sem responsável, mas sempre tem um criador.
- `tarefas` **N:N** `tags_verdes`, via `tarefas_tags`.
- `tarefas_tags` **1:0..1** `registros_impacto` — vínculo pode existir sem estimativa; uma estimativa depende de um vínculo válido.

## 2. Convenções e decisões técnicas

1. **Chaves primárias:** UUID; em PostgreSQL, `gen_random_uuid()` é uma alternativa.
2. **Data/hora:** `TIMESTAMPTZ` para eventos auditáveis; `DATE` para prazo de tarefas que não tenha horário.
3. **Autorização:** o papel fica em `membros`, e não diretamente em `usuarios`, permitindo papéis diferentes por organização. No backend, conferir a organização do usuário em toda leitura/mutação.
4. **Integridade por organização:** além de verificar no Spring Security/Service, considerar FKs compostas `(organizacao_id, membro_id)` nas tarefas, eventos e demais tabelas; a referência simples de `membro_id` **não impede sozinha** relacionamentos entre empresas diferentes.
5. **Senhas:** salvar hash (BCrypt/Argon2id), nunca senha em texto puro ou criptografia reversível.
6. **Status/prioridade/papel:** documentar valores permitidos e adicionar `CHECK` no PostgreSQL ou enum conforme decisões do backend.
7. **Impacto ESG:** não calcular CO₂ ou folhas poupadas sem valor de entrada, fonte e fatores explicáveis. `valor_estimado = quantidade_base × fator_conversao` apenas quando a métrica escolhida seguir esse modelo.
8. **LGPD:** evitar CPF e outros dados pessoais sem necessidade do problema; aplicar princípios de minimização e proteção.

## 3. Decisões ainda pendentes

- [ ] O piloto é **uma empresa só**? Recomendação: manter `organizacoes`/`membros` desde o começo, mas simplificar a interface.
- [ ] Quem pode atribuir uma tarefa, alterar o responsável, editar e excluir?
- [ ] Quais estados e transições de tarefa serão usados? Uma tarefa pode ter vários responsáveis?
- [ ] Prazo terá somente data ou também hora? Como tratar fusos horários?
- [ ] Quais tags sustentáveis entram no MVP? Qual evidência é necessária para contabilizar impacto?
- [ ] Como medir estimativa de papel evitado e registrar fonte/fator sem criar valores fictícios?

## 4. Para usar no GitHub e no desenvolvimento

- Este é o documento principal do modelo de dados. Não mantenha cópias em outros diretórios.
- A visualização Mermaid renderiza direto no GitHub.
- Valide o DER antes de gerar migrações com **Flyway** ou **Liquibase** e implementar entidades JPA.
- Modelos de módulos ainda não priorizados ficam em [`futuro/`](futuro/).
