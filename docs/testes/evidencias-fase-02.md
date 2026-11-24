# Evidências de validação — Fase 02

**Data:** 24 de novembro de 2026 · **Versão:** Acorde 2.0 · **Escopo:** RF-v2-01 e RF-v2-02.

## Backend e PostgreSQL

- `backend/mvnw.cmd test -q` passou: 8 testes, 0 falhas, 0 erros. A suíte sobe o Spring Boot com H2 isolado por padrão para validação rápida.
- A mesma suíte foi executada com `ACORDE_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55432/acorde`, `ACORDE_TEST_DDL_AUTO=validate` e `ACORDE_TEST_FLYWAY_ENABLED=true` contra um cluster PostgreSQL 18.6 temporário criado para a validação: 8 testes, 0 falhas, 0 erros.
- Flyway executou `V1` e `V2` com sucesso no PostgreSQL 18.6; `flyway_schema_history` registrou ambas com `success=true`. Hibernate validou o mapeamento (`ddl-auto=validate`). Tabelas resultantes: `organizacoes`, `usuarios`, `membros`, `sessoes_autenticacao` e `flyway_schema_history`.
- API Spring Boot iniciada conectada a PostgreSQL 18.6 e smoke check HTTP passou: `/api/health` retornou `UP`; login GESTOR retornou token; lista devolveu dois membros da própria organização; COLABORADOR recebeu 403; leitura cross-organization recebeu 404; logout respondeu 204 e o mesmo token passou a receber 401.
- Perfil `local-demo` iniciado num banco PostgreSQL isolado vazio, aplicou migrations e criou duas organizações/quatro contas fictícias. A senha da massa local veio de variável de ambiente temporária e foi armazenada como BCrypt; não foi versionada. As credenciais fixas da suíte são sintéticas e limitadas ao fixture de teste.
- T-01/T-02: login válido, credencial inválida e seleção de organização não vinculada.
- T-03/T-04: autorização de gestor, negação a colaborador e tentativas cross-organization de leitura e alteração.
- Cenários adicionais: JWT inválido/expirado, endpoint autenticado sem bearer, logout com revogação imediata, alteração de papel invalidando sessões anteriores e proteção do último gestor.

O PostgreSQL 18.6 temporário foi encerrado após o teste. O serviço PostgreSQL 18 já existente no computador permaneceu intacto. A pasta temporária isolada não foi removida: o ambiente bloqueou a limpeza recursiva automática. Docker não está instalado, então `docker compose up` não foi validado; a conexão, as migrações e os cenários foram verificados diretamente em PostgreSQL isolado.

## Clientes

- `frontend/npm run build`: passou (TypeScript e Vite).
- `mobile/flutter analyze`: passou, sem issues.
- `mobile/flutter test`: passou, 1 widget test para formulário inicial acessível de login.
- O login foi exercitado por HTTP contra o backend/PostgreSQL; as telas Web/Mobile passaram build/análise, mas não houve teste E2E com navegador/dispositivo real nesta fase.

## Limites

Os testes não cobrem criação/convite de usuários, recuperação de senha, MFA, refresh token, CORS de produção nem autorização de módulos de negócio. O MVP exige novo login quando o access token de 15 minutos expira. Compatibilidade de release Android/iOS e conformidade integral WCAG/eMAG continuam sem validação.
