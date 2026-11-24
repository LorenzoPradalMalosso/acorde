# Relatório de reformulação — Acorde 2.0

**Data:** 24 de novembro de 2026  
**Versão do sistema:** Acorde 2.0  
**Estado atual:** reformulação documental concluída; Fase 2 de implementação registrada abaixo
**Referência conceitual:** `Acorde_2.0_Contexto_Codex.pdf`

## 1. Resumo da mudança

O Acorde 2.0 passa a ter como objetivo principal **monitorar fontes selecionadas da pegada de carbono operacional e apoiar ações de redução**. Gestão de atividades e colaboração passam a servir ao registro de dados e à execução de ações ambientais. O sistema não se apresenta como inventário corporativo completo.

O MVP proposto monitora energia elétrica e deslocamentos casa–trabalho, calcula estimativas rastreáveis em kg CO₂e, apresenta histórico e indicadores ambientais e permite acompanhar metas e ações de redução.

## 2. Escopo desta atualização documental

Foram alinhados ao posicionamento 2.0: README, registro de decisões, requisitos funcionais e não funcionais, escopo, validação do problema, cronograma, arquitetura, DER, expansões de dados, protótipos, plano de testes e modelo de relatórios de sprint. O Brand Kit e a stack tecnológica não foram alterados.

O documento original RF01–RF11 continua registrado como baseline. O catálogo novo usa IDs `RF-v2-01` a `RF-v2-09`, com uma matriz de correspondência para evitar colisões e ocultação de requisitos. O novo DER é uma proposta conceitual; não há banco, entidades JPA, endpoints ou telas implementados.

## 3. Diretrizes metodológicas incorporadas

- Cálculo `E = Q × FE`, somente com quantidade e fator compatíveis.
- Fatores com fonte, versão, validade, unidade, escopo, região e premissas; o cálculo guarda o fator utilizado.
- Registro sem dados suficientes fica pendente, não recebe valor zero artificial.
- Histórico mantém cálculos anteriores; revisões não podem duplicar os totais vigentes.
- Painel comunica cobertura e caráter parcial dos dados; compara apenas períodos e métodos compatíveis.
- Emissões contabilizadas, cenários hipotéticos, emissões evitadas e conclusão de ações são conceitos separados.
- Dados pessoais são minimizados; não se presume necessidade de endereço ou GPS.

Os fatores reais e sua metodologia ainda precisam de pesquisa e aprovação. Valores ilustrativos do PDF são fictícios e não servem para produção.

## 4. Decisões preservadas

- React + TypeScript + Vite para Web; Flutter para Mobile.
- Java + Spring Boot, API REST HTTP/JSON e PostgreSQL.
- Spring Security + JWT; monólito modular, arquitetura em camadas e referência MVC.
- Identidade Acorde, paleta, logotipos, tipografia e tokens oficiais.
- RNFs de desempenho, acessibilidade, usabilidade, multiplataforma, Green IT e segurança/LGPD.

## 5. Validações pendentes e riscos

O piloto e a necessidade ainda não foram validados em campo. Faltam aprovação acadêmica formal do escopo, seleção de fatores e métodos de cálculo, RBAC/estados para os módulos de negócio e avaliação da capacidade de entregar duas fontes, duas interfaces, cálculo, dashboard, metas e ações em seis semanas. RBAC de membros e contratos REST de autenticação foram formalizados para a Fase 2; Flyway foi escolhido em D-004.

O cronograma 2.0 deve ser tratado como plano proposto. Caso a equipe não consiga manter a jornada integrada com qualidade, reduzir o escopo e registrar a decisão com o professor, sem alegar cobertura ou impacto que os dados não sustentem.

## 6. Estado de implementação

A base de Web, Mobile e API e os fluxos de autenticação/organizações foram implementados. Migrations `V1`/`V2`, API conectada, login, sessões, RBAC e isolamento foram verificados em PostgreSQL 18.6 isolado; detalhes e limites estão em [`../testes/evidencias-fase-02.md`](../testes/evidencias-fase-02.md). Nenhum módulo de carbono foi implementado. Protótipos e validação de campo permanecem pendentes.

## 7. Documentos de referência

- [`README.md`](../../README.md)
- [`decisoes.md`](../decisoes.md)
- [`escopo-mvp.md`](escopo-mvp.md)
- [`requisitos-funcionais.md`](../requisitos/requisitos-funcionais.md)
- [`arquitetura.md`](../arquitetura/arquitetura.md)
- [`DER_Acorde.md`](../der/DER_Acorde.md)
- [`plano-testes.md`](../testes/plano-testes.md)
