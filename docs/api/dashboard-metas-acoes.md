# API de dashboard, metas e ações — Acorde 2.0

**Versão:** 2.0 · **Estado:** endpoints implementados e cobertos por testes PostgreSQL. Os estados e a matriz de permissões de negócio seguem a proposta provisória D-012 e aguardam validação em P-004.

Todas as rotas abaixo exigem `Authorization: Bearer <accessToken>`. Organização e membro vêm das claims de sessão validadas pelo backend. Nenhuma rota aceita `organizationId` do cliente.

## Dashboard

### `GET /api/dashboard`

Requer perfil `GESTOR`. Retorna totais da organização autenticada, agrupados por fonte e pelos intervalos de atividade informados. Pode receber `from` e `to` juntos como datas ISO inclusivas; atividades parcialmente fora do intervalo não entram. Fornecer apenas uma data ou intervalo invertido retorna `400 validation_error`.

Exemplo estrutural com números sintéticos, apenas para ilustrar o contrato:

```json
{
  "from": null,
  "to": null,
  "activityCount": 2,
  "pendingCount": 1,
  "emissionsKgCo2e": 10.00000000,
  "sources": [{
    "category": "ENERGIA_ELETRICA",
    "activityCount": 2,
    "validCount": 1,
    "pendingCount": 1,
    "emissionsKgCo2e": 10.00000000
  }],
  "periods": [{
    "periodStart": "2026-02-01",
    "periodEnd": "2026-02-28",
    "activityCount": 1,
    "validCount": 1,
    "pendingCount": 0,
    "emissionsKgCo2e": 10.00000000
  }],
  "coverageNote": "Total parcial das fontes monitoradas..."
}
```

Somente atividades no estado atual válido e o respectivo cálculo vigente entram na soma. Pendências são contadas separadamente e não viram zero individual nem entram no total de emissões. Atividades substituídas ficam fora. Quando não existe cálculo válido no recorte, `emissionsKgCo2e` é `null`; a interface apresenta “Sem cálculo válido”/“Sem dados”, nunca zero artificial. Com as constraints atuais de quantidade e fator positivos, um cálculo válido produz resultado positivo. A nota de cobertura explicita a natureza parcial dos dados.

A API preserva os períodos informados e não os distribui artificialmente por mês. Não calcula variação ou percentual de redução: não existe ainda evidência de cobertura e metodologia comparáveis entre períodos.

## Metas

### `POST /api/goals`

Requer `GESTOR`; cria meta na organização autenticada, inicialmente no estado `ATIVA`.

```json
{
  "category": "ENERGIA_ELETRICA",
  "basePeriodStart": "2025-01-01",
  "basePeriodEnd": "2025-12-31",
  "baseValueKgCo2e": "0",
  "targetPercent": "15",
  "dueDate": "2027-12-31"
}
```

`category` admite `ENERGIA_ELETRICA` ou `DESLOCAMENTO`; intervalo-base não pode ser invertido; `baseValueKgCo2e` aceita zero ou valor positivo; `targetPercent` deve ser maior que zero e no máximo 100. A rota não calcula progresso. Com base zero, progresso percentual é indefinido e não se divide por zero.

### `GET /api/goals`

Requer `GESTOR`. Lista somente metas da organização da sessão.

### `PATCH /api/goals/{goalId}/state`

Requer `GESTOR`. Atualiza o estado da meta para `ATIVA`, `CONCLUIDA` ou `CANCELADA`, sempre limitado à organização autenticada. Esta mudança é operacional e não calcula percentual de redução.

## Ações

### `POST /api/actions`

Requer `GESTOR`. A meta e o membro responsável precisam pertencer à organização da sessão; o responsável deve estar ativo.

```json
{
  "goalId": "uuid-da-meta",
  "responsibleMemberId": "uuid-do-membro",
  "title": "Revisar iluminação",
  "description": "Descrição opcional",
  "dueDate": "2027-06-30"
}
```

Nova ação começa em `PLANEJADA`.

### `GET /api/actions`

Gestor recebe ações da própria organização; colaborador recebe somente as ações atribuídas ao seu `member_id`.

### `PATCH /api/actions/{actionId}/state`

Gestor pode atualizar uma ação da própria organização. Colaborador só pode atualizar a própria ação atribuída e, na proposta D-012, avançar `PLANEJADA` para `EM_ANDAMENTO`, depois `EM_ANDAMENTO` para `CONCLUIDA`. Cancelamento e reabertura cabem ao gestor. A conclusão não altera fator, atividade, cálculo ou emissão.

Estados de meta: `ATIVA`, `CONCLUIDA`, `CANCELADA`. Estados de ação: `PLANEJADA`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA`. A aprovação dessa lista/transições segue pendente em P-004.
