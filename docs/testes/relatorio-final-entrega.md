# Relatório de entrega local — Acorde 2.0

**Data:** 24 de novembro de 2026  
**Escopo:** fases 05–09, retomada local  
**Estado:** código pronto para execução e demonstração local controlada; gates de aceite acadêmico, ambiental, de dispositivo e de operação continuam abertos.

## Entrega

O repositório contém backend Spring Boot, PostgreSQL/Flyway, cliente Web React, aplicativo Flutter, contratos da API, Compose de desenvolvimento e preparação de produção, CI e documentação de execução. As alterações estão no working tree e não foram commitadas.

A demonstração ponta a ponta disponível usa dados de teste sintéticos. O perfil `local-demo` só cria fatores identificados como sintéticos quando o banco está vazio e o catálogo não tem fatores. Não há fatores reais aprovados; nenhuma estimativa obtida com esses valores pode ser apresentada como resultado ambiental real.

## Classificação dos requisitos

| Requisito | Estado | Evidência e limite conhecido |
|---|---|---|
| RF-v2-01 — autenticação e perfis | Implementado e validado na API | Suíte PostgreSQL cobre login, sessão, papéis e acesso negado. Jornada visual completa ainda sem validação manual. |
| RF-v2-02 — organizações e membros | Implementado e validado na API | Testes cobrem isolamento entre organizações e membros. |
| RF-v2-03 — registro de atividades | Parcialmente implementado | API de energia/deslocamento, histórico autorizado, idempotência e formulário Mobile existem; fluxos finais no navegador/dispositivo não foram percorridos manualmente. |
| RF-v2-04 — fatores de emissão | Parcialmente implementado | Catálogo versionado de consulta e seleção no cálculo existem; carga é controlada, sem interface de gestão e sem fatores reais aprovados (P-003). |
| RF-v2-05 — cálculo rastreável | Parcialmente implementado | API e teste sintético validam `E = Q × FE`, snapshots e pendência sem fator. Uso ambiental real depende da aprovação de fontes e método (P-003). |
| RF-v2-06 — dashboard e histórico | Parcialmente implementado | Agregação e builds Web/API validados; cobertura e pendências são separadas. Interação visual completa não foi validada. |
| RF-v2-07 — metas de redução | Parcialmente implementado | CRUD de escopo MVP e transições têm cobertura de API; regras de negócio D-012 aguardam aprovação P-004 e não há progresso ambiental sem método comparável. |
| RF-v2-08 — planos de ação | Parcialmente implementado | API, controle por papel e telas Web/Mobile existem. A sincronização do fluxo visual Mobile → Web não foi exercitada como jornada de interface; D-012/P-004 continua provisória. |
| RF-v2-09 — indicadores ambientais | Parcialmente implementado | Dashboard informa cobertura e não inventa comparação; comparações ficam indisponíveis até metodologia/cobertura compatíveis serem aprovadas (P-003). |
| RNF01 — desempenho | Parcialmente implementado | Stack e consultas funcionam localmente; não há medição reproduzível para o limite de 5 segundos nem teste de carga. |
| RNF02 — acessibilidade | Parcialmente implementado | Há rótulos, feedback e semântica básica no código; WCAG/eMAG, teclado, contraste e leitor de tela não foram auditados manualmente. |
| RNF03 — usabilidade/responsividade | Parcialmente implementado | Web tem layout responsivo e correção de overflow; a captura visual após a correção foi bloqueada e as jornadas não foram revisadas manualmente. |
| RNF04 — multiplataforma | Parcialmente implementado | Web e APK Android compilam; não há validação em dispositivo/emulador Android nem build/teste iOS registrado. |
| RNF05 — Green IT/tema escuro | Parcialmente implementado | Web e Mobile permitem tema escuro; o Mobile tem teste de widget da alternância. Otimização de consultas e consumo não foram medidos. |
| RNF06 — segurança e LGPD | Parcialmente implementado | Hash de senha, sessão/JWT, autorização no servidor e isolamento têm cobertura automatizada; revisão formal de segurança/privacidade e retenção ainda pendente. |

A cobertura dos cenários T-01 a T-18, incluindo estados parciais e pendências, está em [`plano-testes.md`](plano-testes.md). A baseline RF01–RF11 permanece preservada no catálogo de requisitos.

## Evidências executadas

- Backend: 22 testes de integração com PostgreSQL temporário, 0 falhas e 0 erros; migrations V1–V4 e regras de autorização cobertas.
- Web: `npm run lint` e `npm run build` concluídos com sucesso.
- Mobile: `flutter analyze --no-pub`, `flutter test --no-pub` (2 testes de widget) e `flutter build apk --no-pub` concluídos com sucesso. O APK usa assinatura debug porque não há keystore da equipe.
- Docker: configuração Compose validada; imagens da API/Web e stack isolada subiram com segredos sintéticos. `/api/health` e Web responderam HTTP 200. O fluxo sintético validou login, registros, idempotência, metas/ações na API, dashboard pendente/válido e persistência após restart da API.
- Perfil `local-demo`: fatores de demonstração produziram resultados sintéticos conhecidos e explicitamente rotulados; dados e volume de teste foram isolados.
- Repositório: `git diff --check` sem erro. Avisos de Git referem-se à conversão LF/CRLF.

Consulte [`evidencias-fases-08-09.md`](evidencias-fases-08-09.md) para detalhes. O container PostgreSQL de desenvolvimento `acorde-postgres` não foi alterado. Os containers do projeto isolado foram parados e seu volume sintético foi preservado. Nenhum push, merge ou deploy externo foi feito.

## Pendências para aceite integral

1. Aprovar recorte acadêmico, piloto e método/fontes de fatores ambientais (P-001/P-002/P-003), antes de apresentar resultados reais.
2. Revisar e aprovar as regras provisórias de metas/ações D-012 (P-004).
3. Percorrer Web e Mobile em navegador e dispositivos/emuladores; validar acessibilidade, falhas de rede e sincronização real do fluxo.
4. Medir RNF01, revisar segurança/privacidade, e ensaiar backup/restauração e rollback.
5. Fornecer keystore Android e decidir alvos iOS, hospedagem, domínio, TLS, segredos e monitoramento antes de qualquer release/deploy.

Essas pendências dependem de validação da equipe, dispositivos, credenciais ou ambiente aprovado; não podem ser marcadas como concluídas apenas por build local.

## Como iniciar a demonstração

Siga o [README principal](../../README.md) para PostgreSQL, API, Web e configuração de dados sintéticos. As instruções específicas de assinatura e execução Mobile estão em [`mobile/README.md`](../../mobile/README.md). Não habilite `local-demo` em produção e não use as credenciais/exemplos sintéticos como dados reais.
