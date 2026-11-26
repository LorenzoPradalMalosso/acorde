# Evidências — Auditoria e preparação de produção (Acorde 2.0)

**Data:** 24 de novembro de 2026 · **Versão:** 2.0

## Fase 08 — validação executada

### Retomada local em 24 de novembro de 2026

- `backend/scripts/test-postgres.ps1`: reexecutado em PostgreSQL 17 temporário; 22 testes, 0 falhas e 0 erros. A primeira tentativa foi bloqueada pelo acesso do sandbox ao Docker; após a autorização do comando, a execução terminou com sucesso. O script criou e removeu apenas o próprio container temporário.
- `frontend`: `npm run lint` e `npm run build` reexecutados com sucesso.
- `mobile`: `flutter analyze --no-pub` e `flutter --version` não produziram saída por mais de um minuto e foram interrompidos. Esta retomada não confirma o estado do SDK nem revalida o Mobile; a evidência anterior abaixo permanece identificada como execução anterior.
- A saída da execução Maven teve logs DEBUG extensos apesar de `application-test.yml` declarar `root: WARN`; investigar a origem da configuração de log antes de considerar concluída a auditoria de privacidade dos logs.

- `backend/scripts/test-postgres.ps1`: passou com PostgreSQL 17 efêmero, sem volume persistente, migrations Flyway habilitadas; 22 testes, 0 falhas/erros. O script encerrou e removeu somente o container que criou; o serviço PostgreSQL de desenvolvimento preexistente permaneceu ativo.
- `frontend`: `npm run lint` e `npm run build` passaram.
- `mobile`: `flutter analyze --no-pub` sem issues e `flutter test --no-pub` passou (1 teste de widget de login).
- `git diff --check` passou sem erro de whitespace. Avisos restantes são apenas conversão LF/CRLF do Git em arquivos já modificados.

## Fase 09 — preparação local validada

- `docker compose --env-file .env.production.example -f docker-compose.production.yml config --quiet` passou usando valores temporários no processo local, sem iniciar containers.
- `docker compose config --quiet` para o compose de desenvolvimento passou.
- Workflow CI, Dockerfiles da API/Web e guia de deploy/backup foram adicionados. O workflow remoto não foi executado e as imagens de produção não foram construídas nesta sessão.

## Pendências/gates

- A Web teve smoke HTTP e screenshot headless da tela de login, mas os fluxos interativos completos não foram exercitados em navegador; o screenshot posterior à correção móvel foi bloqueado pela política automática do ambiente.
- Mobile não foi executado em emulador/dispositivo Android/iOS; Flutter analyze e widget test não comprovam esses alvos.
- Falhas de rede E2E, leitores de tela/contraste manual, desempenho e recuperação de backup ainda precisam ser medidos.
- Provedor, domínio, TLS, gestão real de segredos e publicação dependem de aprovação e ambiente externo. Nenhum deploy, push ou alteração de infraestrutura externa foi feito.
- Fatores ambientais reais, validação acadêmica e aprovação da regra provisória D-012 continuam pendentes em P-003/P-004.

## Finalização local em 24 de novembro de 2026

- Backend: `backend/scripts/test-postgres.ps1` passou após ajuste no tratamento de avisos nativos do Maven; 22 testes, 0 falhas e 0 erros. O script conserva/restaura variáveis do processo, desativa `DEBUG` durante a suíte e usa somente seu PostgreSQL temporário.
- Web: `npm run lint` e `npm run build` passaram após correção do overflow móvel. As imagens Docker de Web/API foram construídas; o erro inicial do Dockerfile da Web (contexto não incluía o token oficial) foi corrigido com contexto raiz enxuto.
- Compose: build e subida isolada de produção passaram com credenciais sintéticas. API e Postgres ficaram sem portas publicadas; Web foi publicada apenas em `127.0.0.1`. Migrations V1–V4 aplicaram em banco vazio; Web e `/api/health` via nginx responderam HTTP 200.
- Jornada API sintética no banco isolado: login gestor/colaborador, cadastro de energia e deslocamento, mesma chave idempotente sem duplicar, criação/avanço de ação, dashboard com pendências sem emissão zero e persistência após restart da API passaram.
- Perfil `local-demo`: passou a carregar dois fatores fictícios somente quando o catálogo está vazio, com fonte/versão/premissas explicitamente sintéticas. Com 100 kWh, a API calculou 10 kg CO₂e; com 100 passageiro-km, 5 kg CO₂e. São fixtures locais, não resultados ambientais reais.
- Após reiniciar novamente API e stack, o catálogo permaneceu com 2 fatores sintéticos e o dashboard manteve `pendingCount=2`, `emissionsKgCo2e=15.00000000`; `/api/health` retornou HTTP 200.
- Flutter: `flutter analyze --no-pub` sem issues, `flutter test --no-pub` passou com 2 testes (login acessível e alternância claro/escuro) e `flutter build apk --no-pub` gerou `mobile/build/app/outputs/flutter-apk/app-release.apk`. Sem keystore externa, o APK usa chave debug e não é release oficial.
- Inspeção visual: screenshot headless desktop da tela de login mostrou marca e formulário renderizados; no viewport móvel foi detectado overflow horizontal, corrigido com `min-width: 0`. O screenshot posterior à correção não foi obtido porque o ambiente bloqueou a automação de navegador.
- O stack `acorde-final-validation` usou volume isolado com dados exclusivamente sintéticos; seus containers foram parados ao encerrar, preservando o volume de teste para inspeção/reuso. Banco/volume `acorde-postgres` existente não foi alterado.

## Requisitos e gates de entrega

- RF-v2-01 a RF-v2-09: implementados no escopo técnico, com testes de API e builds; fluxos visuais em dispositivos não estão integralmente validados.
- RNF01–RNF06: acessibilidade manual, desempenho, recuperação/backup e validação nos alvos Android/iOS ainda não têm medições reproduzíveis; não classificar como validados.
- P-003: fontes/fatores reais, método elétrico e validação acadêmica continuam pendentes; somente dados sintéticos do `local-demo` permitem demonstrar o cálculo.
- P-004/D-012: matriz e transições de metas/ações permanecem provisórias.
- Assinatura Android de distribuição, TLS/provedor e deploy externo dependem de ambiente/chaves/aprovação da equipe.

O checklist consolidado RF-v2-01–09/RNF01–06 e o estado de aceite estão em [`relatorio-final-entrega.md`](relatorio-final-entrega.md).

## Verificação dos cálculos em 26 de novembro de 2026

- O banco `acorde` tinha atividade pendente e catálogo vazio. A demonstração usou o banco dedicado `acorde_demo_calculos`, povoado pelo perfil `local-demo`.
- Pelo proxy Web local, as duas contas da organização A autenticaram. A API calculou **10 kg CO₂e** para 100 kWh e **5 kg CO₂e** para 100 passageiro-km. O dashboard retornou **15 kg CO₂e**, 2 atividades e 0 pendências. Os dois históricos indicaram o fator sintético `TESTE-LOCAL-1`.
- **23 testes PostgreSQL passaram**. O cenário de regressão confirma que a mesma atividade passa de pendente para válida após a carga de um fator compatível, com nova revisão e autorização do autor. Lint e build Web, análise Flutter e os 2 testes de widget passaram. O botão de recálculo no Mobile ainda não foi percorrido em dispositivo.

## Cadastro de deslocamento na Web (D-014)

- Lint e build da Web passaram. Pelo proxy local, o login do colaborador retornou o papel `COLABORADOR` e a consulta retornou seu registro de exemplo. A submissão pelo formulário ainda precisa de verificação manual no navegador.
- Os 23 testes de integração com PostgreSQL passaram, incluindo registro de deslocamento com e sem fator compatível. A análise Flutter e os 3 testes de widget passaram após a inclusão do atalho em **Registros**.

## Ações atribuídas na Web (D-015)

- Lint e build da Web passaram. A consulta pelo proxy local retornou apenas as duas ações atribuídas ao colaborador de demonstração.
- Os 23 testes com PostgreSQL passaram. A suíte cobre o avanço de planejada para em andamento e depois concluída, a restrição por responsável e a rejeição de transições indevidas. A interação com os botões no navegador ainda precisa de verificação manual.
