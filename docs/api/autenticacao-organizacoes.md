# API de autenticação e organizações — Acorde 2.0

**Estado:** implementado para RF-v2-01/RF-v2-02 no backend Spring Boot. A API usa JSON em `/api`; autenticação usa bearer token JWT.

## Sessão e segurança

- Login por e-mail/senha; senhas armazenadas com BCrypt (custo 12). E-mail é normalizado para minúsculas. A mensagem para e-mail/senha/membro inválidos é uniforme.
- A resposta do login contém JWT HS256 válido por 15 minutos. O segredo HMAC deve ser Base64 com pelo menos 32 bytes aleatórios e vem de `JWT_SECRET_BASE64`; nunca reutilize o valor de exemplo.
- Cada sessão é vinculada ao membro e persistida em `sessoes_autenticacao`. A API consulta a sessão e os estados do usuário/vínculo em cada validação do JWT. Logout revoga a sessão no servidor imediatamente; mudança de papel também revoga as sessões do membro alterado.
- Sessões expiradas e revogadas são removidas periodicamente; a validação exige registro de sessão ativo e `exp` válido.
- O MVP não emite refresh token. Após 15 minutos ou ao reiniciar Web/Mobile, a pessoa autentica novamente. Os clientes mantêm o access token apenas em memória e o descartam ao sair/expirar.
- Produção deve usar HTTPS e segredo próprio, rotacionável e guardado fora do repositório. Não habilite o perfil `local-demo` fora do desenvolvimento local.

## Matriz RBAC implementada nesta fase

| Operação | Anônimo | GESTOR | COLABORADOR |
|---|---:|---:|---:|
| `GET /api/health` | Permitido | Permitido | Permitido |
| `POST /api/auth/login` | Permitido | Permitido | Permitido |
| `GET /api/auth/me` | 401 | Próprio perfil e vínculo | Próprio perfil e vínculo |
| `POST /api/auth/logout` | 401 | Revoga a própria sessão | Revoga a própria sessão |
| `GET /api/organizations/{organizationId}/members` | 401 | Lista membros da organização da sessão | 403 |
| `PATCH /api/organizations/{organizationId}/members/{memberId}/role` | 401 | Altera papel de outro membro ativo da própria organização | 403 |

O identificador de organização no caminho é conferido contra o vínculo gravado e validado no login; ele nunca define o escopo sozinho. A consulta de membros filtra pela organização autenticada e o membro alvo do PATCH é carregado por `(memberId, organizationId)`. Acesso a outra organização retorna 404 para não revelar se o recurso existe. Um gestor não pode alterar o próprio papel nem rebaixar o último gestor ativo. Mudanças de papel invalidam as sessões anteriores do membro.

Esta matriz cobre as permissões formalizadas para autenticação e organizações. Os módulos de negócio foram implementados depois com base nos fluxos do MVP; sua matriz e estados permanecem provisórios conforme D-008/D-012 e P-004.

### RBAC dos módulos de negócio (implementado provisoriamente)

Esta matriz registra o comportamento atual dos endpoints. D-012 continua aguardando revisão; implementação não equivale a aprovação de produto ou acadêmica.

| Domínio/ação | GESTOR | COLABORADOR | Estado |
|---|---|---|---|
| Energia elétrica | Registra e consulta dados autorizados da organização | Sem escrita de energia | Implementado; endpoint exige GESTOR |
| Deslocamento casa–trabalho | Consulta atividades da organização | Registra e consulta os próprios deslocamentos | Implementado; detalhe individual fica restrito ao próprio membro |
| Fatores de emissão | Consulta; sem edição | Consulta; sem edição | Catálogo somente leitura; inclusão curada ainda depende de P-003 |
| Cálculos | Gerados no backend com snapshots de fator e atividade | Sem edição direta | Implementado; dados incompatíveis permanecem pendentes |
| Dashboard | Consulta indicadores agregados da própria organização | Sem acesso ao painel gerencial | Implementado; totais parciais e pendências explícitas |
| Metas | Cria e acompanha metas da organização | Sem gestão direta | Implementado conforme D-012 provisória |
| Ações | Cria, atribui e acompanha ações | Consulta e avança somente ações atribuídas | Implementado conforme D-012 provisória |

Transições de metas/ações seguem provisoriamente D-012. A manutenção de fatores reais segue pendente em P-003; não existe endpoint de edição do catálogo.

## Endpoints

### `POST /api/auth/login`

Request:

```json
{
  "email": "gestor@example.test",
  "password": "senha informada localmente",
  "organizationId": "uuid-opcional"
}
```

`organizationId` é opcional e serve apenas para selecionar um vínculo existente. O servidor valida usuário, senha e vínculo ativo antes de emitir token. Com um vínculo ativo, a organização é selecionada automaticamente. Com vários vínculos e sem seleção, responde `409 organization_selection_required` incluindo as opções que pertencem ao usuário autenticado. Um UUID de outra organização ou credenciais inválidas respondem `401 invalid_credentials`.

Resposta `200`:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresAt": "2026-11-24T15:15:00Z",
  "profile": {
    "userId": "uuid",
    "name": "Nome",
    "email": "gestor@example.test",
    "organizationId": "uuid",
    "organizationName": "Organização",
    "memberId": "uuid",
    "role": "GESTOR"
  }
}
```

### `GET /api/auth/me`

Requer `Authorization: Bearer <accessToken>`. Devolve o perfil e vínculo atualmente ativos no banco; não usa nome, papel ou organização fornecidos pelo cliente.

### `POST /api/auth/logout`

Requer bearer token. Responde `204 No Content` e revoga o `jti` persistido. Reapresentar o mesmo token retorna `401`.

### `GET /api/organizations/{organizationId}/members`

Requer papel `GESTOR` e organização igual à sessão. Responde lista de `id`, `name`, `email`, `role` e `joinedAt`. Não retorna hashes de senha nem membros inativos.

### `PATCH /api/organizations/{organizationId}/members/{memberId}/role`

Requer papel `GESTOR` e escopo da própria organização.

```json
{ "role": "COLABORADOR" }
```

Papel deve ser `GESTOR` ou `COLABORADOR`. Responde `200` com o membro atualizado. Alterar o próprio papel ou remover o último gestor ativo responde `409` (`self_role_change_not_allowed` ou `last_manager_required`). Membro de outra organização ou inexistente responde `404 resource_not_found`.

## Erros padronizados

```json
{ "status": 403, "code": "forbidden", "message": "A operação não é permitida para este perfil" }
```

O mesmo formato é usado para `400 validation_error`/`invalid_request`, `401 invalid_credentials`/`unauthorized`, `404 resource_not_found`, `409` e `500 internal_error`. Respostas 500 não incluem stack trace, SQL, tokens ou dados pessoais.

## Dados isolados de demonstração e testes

`local-demo` é um perfil explícito. Execute `backend/run-local.ps1 -DemoData` após configurar uma senha local de pelo menos 12 bytes UTF-8 e no máximo 72 bytes em `.env` (`ACORDE_DEMO_PASSWORD`). Em banco vazio, cria duas organizações e quatro contas fictícias: `gestor-a@acorde.local`, `colaborador-a@acorde.local`, `gestor-b@acorde.local`, `colaborador-b@acorde.local`; todas usam a senha local informada e são armazenadas como BCrypt. No mesmo banco novo, se o catálogo estiver vazio, carrega dois fatores sintéticos para `BR-SP`, com fonte, versão e premissas que identificam claramente que são fictícios e exclusivos de teste local. Não são fatores ambientais reais e não devem ser usados em produção. Se já houver organizações, usuários ou membros, o perfil retorna sem inserir ou alterar dados.

Os testes atuais usam massa isolada (`@example.test`), senha sintética e PostgreSQL efêmero dedicado; nenhuma credencial de teste serve como credencial de ambiente. Ver evidências de cada fase em `docs/testes/`.
