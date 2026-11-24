# Evidências de implementação — Fase 03

**Versão:** Acorde 2.0 · **Data:** 24/11/2026 · **Estado:** motor e contratos exercitados em PostgreSQL; fatores reais e validação acadêmica permanecem pendentes.

## Implementação validada

- Migration V3 cria catálogo, atividades tipadas, cálculos e índices de integridade. Corrigidas a chave única composta necessária à FK organizacional e a regra de estado que impedia preservar resultado em revisões substituídas.
- Região de deslocamento é persistida junto aos dados tipados para validação do fator e reprocessamento.
- Fator precisa corresponder a categoria, escopo provisório, unidade, unidade de resultado `kg CO2e`, região e todo o período da atividade. Região específica precede `*`; ambiguidade mantém pendência.
- Energia é calculada em `BigDecimal` como `Q × FE`, arredondada a 8 casas com `HALF_UP`; fatores, entradas, período e região usados ficam no snapshot da revisão.
- Ausência/incompatibilidade mantém a revisão pendente sem resultado numérico. O catálogo não expõe escrita aos perfis de produto.
- Reenvio com a mesma chave de idempotência não cria outra atividade. Revisão da atividade e reprocessamento são explícitos, preservam histórico e mantêm no máximo uma revisão válida vigente.
- Acesso à revisão e ao reprocessamento é escopado no backend por membro, organização e papel da fonte; o ator atual só pode revisar/reprocessar atividade própria.

## Execução automatizada

A suíte `AuthenticationApiIntegrationTest` foi executada contra PostgreSQL 17 temporário e isolado em `acorde_test`, com Flyway ativo e Hibernate em `validate`:

```powershell
$env:ACORDE_TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:55439/acorde_test'
$env:ACORDE_TEST_DB_USERNAME = 'acorde_test'
$env:ACORDE_TEST_DB_PASSWORD = '<senha local temporária>'
$env:ACORDE_TEST_FLYWAY_ENABLED = 'true'
$env:ACORDE_TEST_DDL_AUTO = 'validate'
cd backend
.\mvnw.cmd test
```

**Resultado:** 20 testes, 0 falhas, 0 erros, 0 ignorados. A fixture de integração recusa rodar fora do banco chamado `acorde_test`, pois recria dados de autenticação e limpa tabelas ambientais antes de cada caso. Nenhum fator real ou segredo de ambiente foi gravado no repositório.

Cobertura acrescentada: energia pendente sem fator, fatores sintéticos com resultado decimal conhecido, unidade/escopo/validade incompatíveis, fatores ambíguos, persistência do fator usado, revisão de atividade, reprocessamento com troca de fator regional, histórico sem reescrita, chave idempotente, deslocamento em passageiro-km e isolamento entre organizações.

## Limitações e bloqueios

- As fixtures são sintéticas e só existem na base temporária de testes. Nenhum fator ambiental real foi publicado.
- O mapeamento de energia a `ESCOPO_2` e deslocamento a `ESCOPO_3` está registrado provisoriamente em D-011; fatores de eletricidade location/market-based, fontes nacionais, escopo acadêmico e premissas exigem validação em P-003 antes de carga real.
- A matriz RBAC ampla de negócio continua pendente em P-004. PUT e reprocessamento foram restringidos ao autor e ao papel atual da fonte como salvaguarda provisória; não se habilitou reprocessamento por terceiros.
- A chave de idempotência protege reenvio acidental com a mesma chave. Não deduplica submissões com chaves diferentes, pessoas diferentes ou caronas; o limite está documentado em D-009.
- A ferramenta de integração executada foi PostgreSQL 17 local temporário. Esta evidência não comprova a imagem Docker Compose nem o fluxo no navegador.
