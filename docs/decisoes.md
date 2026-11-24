# Registro de decisões do projeto — Acorde 2.0

> Este registro distingue decisões de produto, propostas técnicas e validações ainda pendentes. A reformulação da versão 2.0 foi autorizada pelo solicitante para orientar a documentação e o desenvolvimento. A validação acadêmica do recorte, do piloto e da metodologia continua pendente. Nenhuma implementação é comprovada por este documento.

| ID | Data | Tema | Decisão/estado | Justificativa / impacto |
|---|---|---|---|---|
| D-001 | Conforme documentação inicial | Stack | **Definida e preservada:** React/TypeScript/Vite, Flutter, Java/Spring Boot, PostgreSQL, REST, Spring Security + JWT | Web e Mobile compartilham API REST |
| D-002 | Conforme documentação inicial | Arquitetura | **Definida e preservada:** camadas, referência MVC e monólito modular | Organizar módulos por domínio com camadas internas |
| D-003 | 2026-11-24 | Produto Acorde 2.0 | **Aprovada pelo solicitante para orientar a reformulação documental e o desenvolvimento; validação acadêmica pendente.** Foco em monitorar fontes selecionadas de emissões e apoiar ações de redução | Reposiciona o produto; exige nova rastreabilidade RF-v2, revisão do DER, protótipos, testes e cronograma. Não altera stack ou identidade visual |
| P-001 | A registrar | Escopo de seis semanas | **Proposta:** MVP com energia, deslocamento casa–trabalho, cálculo rastreável, dashboard, metas e ações | Avaliar capacidade e validar com professor antes de fixar entregas |
| P-002 | A registrar | Contexto real | **Pendente:** organização/equipe piloto e evidências do problema | Validar necessidade e dados disponíveis sem presumir entrevistas |
| P-003 | A registrar | Metodologia e fatores | **Pendente:** fontes de fatores, método de energia/mobilidade, unidades, regiões e períodos | Não usar fatores fictícios nem afirmar redução sem dados comparáveis |
| P-004 | A registrar | RBAC e estados | **Pendente:** matriz de permissões e estados de atividade, cálculo, meta e ação | Aplicar isolamento por organização e permissões no backend |
| P-005 | A registrar | Contratos e migrações | **Pendente:** endpoints REST, formato de erros, Flyway/Liquibase e ambiente | Definir após validar DER e regras de cálculo |

## Baseline de requisitos

A baseline original `RF01–RF11` e `RNF01–RNF06` permanece registrada para rastreabilidade. Os requisitos `RF-v2-01–RF-v2-09` constituem o catálogo proposto para a versão 2.0; não reutilizam os números antigos. A correspondência está em [`requisitos-funcionais.md`](requisitos/requisitos-funcionais.md). Qualquer requisito acadêmico que precise permanecer na entrega deve ser confirmado antes de sua retirada formal.

## Modelo de nova decisão

### D-XXX — Título
- **Data:**
- **Status:** proposta / aprovada / substituída
- **Contexto:**
- **Alternativas consideradas:**
- **Decisão:**
- **Justificativa:**
- **Impacto em RFs/RNFs, arquitetura, dados e cronograma:**
- **Responsável / aprovação:**
