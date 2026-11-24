# Evidências — Fases 05 a 07 do Acorde 2.0

**Data:** 24 de novembro de 2026 · **Versão:** 2.0

## Alterações entregues

- Mobile Flutter: login/sessão já existente ampliados com registro autenticado de deslocamento, listagem de atividades permitidas e consulta/avanço de ações atribuídas. O fluxo não usa GPS nem endereço. A chave UUID de idempotência fica estável durante a tentativa; uma nova chave só é criada para o próximo envio depois do sucesso.
- Dashboard: `GET /api/dashboard` agrega atividades atuais da organização por fonte e período informado. A soma inclui apenas cálculos `VALIDA` vigentes; pendências são contabilizadas à parte e explicitadas como lacuna de cobertura.
- Metas e ações: migration Flyway V4, endpoints REST, autorização por papel/organização, cadastro/consulta pela Web e atualização de ações próprias no Mobile. Base de meta zero é persistida sem percentual de progresso.
- D-012 registra os estados e a matriz mínima de negócio como proposta provisória em revisão; não há progresso ambiental nem comparação de redução sem metodologia/cobertura comparáveis.

## Validações executadas

- Backend: `mvnw.cmd test`, em PostgreSQL 17 descartável sem volume, com Flyway habilitado. Resultado: 22 testes, 0 falhas, 0 erros. A suíte inclui isolamento do dashboard, filtro temporal, agregação de pendente/válida, acesso de gestor/colaborador, criação de meta-base zero e atribuição/transição de ação.
- Frontend: `npm run lint` e `npm run build` passaram.
- Mobile: `flutter analyze --no-pub` sem issues; `flutter test --no-pub` passou (1 widget test do formulário de login).
- Os fatores usados nos testes de cálculo são fixtures sintéticas marcadas como tal. O fator sintético do dashboard também existe apenas na instância efêmera de teste.

## Limitações ainda abertas

- A integração visual Web e persistência em navegador real, e revisão manual de acessibilidade, não foram executadas nesta sessão.
- O teste Flutter automatizado atual cobre só a tela de login; fluxo de rede, persistência e renderização de ações precisam ser exercitados em dispositivo/emulador Android/iOS.
- RNFs de desempenho, teste E2E de falha de rede e revisão de segurança/privacidade de release ainda não foram medidos.
- Fontes/fatores reais e validação acadêmica do método seguem pendentes em P-003. Estados e matriz D-012 aguardam aprovação em P-004.

Nenhum dado real de emissão ou implantação externa foi criado/afirmado.
