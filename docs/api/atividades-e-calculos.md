# API de atividades e cálculos — Acorde 2.0

**Versão:** 2.0 · **Estado:** contrato implementado em desenvolvimento; escopos e matriz RBAC de negócio ainda aguardam validação acadêmica.

Todas as rotas usam `/api` e exigem `Authorization: Bearer <accessToken>`, salvo indicação em contrário. A organização e o membro são sempre derivados das claims validadas contra a sessão ativa; o cliente não escolhe o escopo organizacional.

## Registro de atividade

### `POST /api/carbon/activities/energy`

Requer perfil `GESTOR`. Registra energia da organização autenticada.

```json
{
  "idempotencyKey": "uuid",
  "periodStart": "2026-01-01",
  "periodEnd": "2026-01-31",
  "region": "BR-SP",
  "consumptionKwh": "125.500000"
}
```

`consumptionKwh` deve ser positivo. Região e período são obrigatórios. O resultado é `201 Created`; repetir a mesma chave para o mesmo membro e organização devolve a atividade original sem criar outra.

### `POST /api/carbon/activities/commutes`

Requer perfil `COLABORADOR`. A atividade pertence ao membro autenticado.

```json
{
  "idempotencyKey": "uuid",
  "periodStart": "2026-01-01",
  "periodEnd": "2026-01-31",
  "region": "BR-SP",
  "mode": "ONIBUS",
  "roundTripDistanceKm": "18.250000",
  "commutingDays": "20"
}
```

A quantidade armazenada em `passageiro-km` é distância de ida e volta multiplicada pelos dias informados, conforme D-009. Ocupação não divide a quantidade quando a unidade do fator é `passageiro-km`. Fator por `veículo-km` não é selecionado por este contrato.

### Resposta

```json
{
  "id": "uuid",
  "category": "ENERGIA_ELETRICA",
  "periodStart": "2026-01-01",
  "periodEnd": "2026-01-31",
  "quantity": 125.500000,
  "unit": "kWh",
  "state": "PENDENTE",
  "justification": "...",
  "revision": 1,
  "emissionsKgCo2e": null,
  "current": false
}
```

Estados de cálculo: `PENDENTE`, `VALIDA` e `SUBSTITUIDA`. Uma pendência nunca retorna emissão zero artificial. Uma revisão válida vigente é a única que pode ser agregada.

## Consulta e histórico

- `GET /api/carbon/activities`: lista as atividades visíveis ao membro autenticado. O comportamento atual de gestor (organização inteira) e colaborador (somente próprias) está sujeito à validação do item P-004.
- `GET /api/carbon/activities/{activityId}`: consulta detalhe; outra organização ou atividade sem permissão retorna 404.
- `GET /api/carbon/activities/{activityId}/calculations`: lista revisões cronológicas com quantidade, unidade, período, região informada, fator, fonte, versão, escopo, região do fator, premissas, resultado, estado e vigência. A validade temporal é consultada no fator imutável referenciado por `factorId`.
- `PUT /api/carbon/activities/{activityId}`: revisa os dados da própria atividade. Mantém a categoria original, recalcula a quantidade a partir dos campos tipados, substitui a revisão anterior e gera uma nova revisão auditável. O papel exigido continua sendo gestor para energia e colaborador para deslocamento.
- `POST /api/carbon/activities/{activityId}/reprocess`: reavalia a atividade com o catálogo atual. Por ora, somente o membro que registrou a atividade pode solicitar o reprocessamento. A matriz completa de permissões de negócio permanece pendente em P-004.

O reprocessamento preserva a revisão anterior como `SUBSTITUIDA`, cria uma nova revisão e atualiza a atividade dentro da mesma transação. A constraint parcial do PostgreSQL impede mais de uma revisão vigente por atividade. Se não houver fator compatível ou único, a nova revisão fica `PENDENTE`, sem resultado.

## Consulta do catálogo

### `GET /api/factors?category=ENERGIA_ELETRICA&region=BR-SP&date=2026-01-31`

Consulta fatores visíveis para a data pedida. O endpoint é somente de leitura; não existe edição de fatores por gestor ou colaborador. Inclusões são cargas controladas pela equipe, conforme D-010. Nenhum fator real foi incluído por esta implementação.

## Seleção e cálculo

O motor centralizado no backend aplica `E = Q × FE` usando `BigDecimal` e persiste valores `NUMERIC`. Um fator somente é selecionado quando categoria, unidade de atividade, escopo, unidade de resultado (`kg CO2e`), região e validade cobrem todo o período da atividade. Região exata tem precedência sobre `*`; mais de um fator candidato na região prioritária torna o cálculo pendente por ambiguidade. O resultado é arredondado a 8 casas decimais com `HALF_UP`.

Para a implementação atual, as categorias são mapeadas provisoriamente a `ESCOPO_2` (energia elétrica) e `ESCOPO_3` (deslocamento casa–trabalho). Essa correspondência segue a classificação do GHG Protocol para eletricidade adquirida e deslocamento de empregados, mas a validação acadêmica do recorte e das opções metodológicas continua pendente em P-003. Não há fatores reais cadastrados; as fixtures automatizadas são sintéticas e isoladas em banco de teste. [GHG Protocol — Scope 2 Guidance](https://ghgprotocol.org/scope-2-guidance), [GHG Protocol — Category 7: Employee Commuting](https://ghgprotocol.org/sites/default/files/standards/Scope3_Calculation_Guidance_0.pdf?3caa887c_page=1).

Fator ausente, unidade ou escopo incompatível, validade parcial ou candidatos ambíguos resultam em estado pendente. Fatores com validade ou versão revisada não sobrescrevem snapshots de cálculo; um reprocessamento explícito cria uma nova revisão.

## Erros principais

As respostas de erro seguem `status`, `code` e `message` conforme o handler comum da API: `400 validation_error`, `401 unauthorized`, `403 forbidden`, `404 resource_not_found` e `409` para conflitos aplicáveis.
