<div align="center">

<img src="assets/branding/Acorde_Brand_Kit/logos/svg/acorde-horizontal-digital.svg" width="680" alt="Acorde - Gestão em sintonia" />

### Gestão em sintonia.

**Monitoramento e redução da pegada de carbono operacional.**

![Versão](https://img.shields.io/badge/VERSÃO-2.0-0F3D34?style=for-the-badge)
![Status](https://img.shields.io/badge/STATUS-EM%20DESENVOLVIMENTO-0F3D34?style=for-the-badge)
![Plataformas](https://img.shields.io/badge/PLATAFORMAS-WEB%20%2B%20MOBILE-1F6B58?style=for-the-badge)

**🌍 Carbono operacional · ⚡ Energia · 🚲 Mobilidade · 🎯 Metas e ações**

</div>

---

> [!IMPORTANT]
> **Versão do sistema: Acorde 2.0.** Esta versão reposiciona o produto para monitorar fontes selecionadas de emissões e apoiar ações de redução. O repositório contém documentação e identidade visual; a implementação de Web, Mobile, backend e banco ainda não foi iniciada. Requisitos, DER e fluxos são propostas de desenvolvimento e não comprovam validação de campo ou fatores de emissão aprovados.

O Acorde é uma plataforma colaborativa para **registrar atividades emissoras, calcular estimativas rastreáveis em kg CO₂e, acompanhar indicadores ambientais e organizar ações de redução**. O MVP prioriza energia elétrica e deslocamentos casa–trabalho. Os cálculos devem informar dados, unidade, fonte, versão e validade do fator utilizado; sem dados suficientes, o registro fica pendente e nenhum impacto é inventado.

## Objetivo e problema

Pequenas e médias organizações podem ter dificuldade para identificar fontes de emissões da rotina de trabalho, acompanhar sua evolução e transformar dados em ações. A necessidade ainda precisa ser validada com uma organização piloto.

**Pergunta-problema:** como auxiliar empresas a identificar, monitorar e reduzir sua pegada de carbono operacional por meio de uma plataforma digital acessível e colaborativa?

O Acorde conecta uma calculadora de carbono, um dashboard ambiental e planos de redução sem se apresentar como inventário corporativo completo. O conceito de **Micro-ESG Operacional** permanece, agora concentrado em carbono.

## Fluxo principal do MVP

```mermaid
flowchart LR
    A[Gestor registra energia mensal na Web] --> B[API valida atividade e fator versionado]
    C[Colaborador registra deslocamento no Mobile] --> B
    B --> D[Backend calcula e persiste estimativa rastreável]
    D --> E[Dashboard exibe total parcial, fontes e histórico]
    E --> F[Gestor define meta e atribui ação]
    F --> G[Colaborador atualiza a ação no Mobile]
    G --> E
```

O total exibido é parcial e depende da cobertura dos registros. Conclusão de uma ação é indicador operacional; não prova redução de emissões.

## Fontes de emissão

| Prioridade | Dados principais | Classificação conceitual |
|---|---|---|
| Energia elétrica | Consumo em kWh, período e localização/rede aplicável | Escopo 2, eletricidade adquirida |
| Deslocamento casa–trabalho | Modalidade, distância de ida e volta, dias/viagens e ocupação quando pertinente | Escopo 3, categoria 7, para veículos não controlados pela organização |
| Expansões | Frota própria, viagens corporativas, papel, materiais e resíduos | Fora do MVP inicial |

`E = Q × FE`, onde `E` é a emissão estimada em kg CO₂e, `Q` é a quantidade da atividade e `FE` é um fator compatível em unidade e período. Exemplos numéricos do PDF de contexto são fictícios e não devem ser usados como fatores de produção.

## Escopo do MVP — seis semanas

- Autenticação e perfis de gestor/colaborador, com isolamento por organização no backend.
- Cadastro Web de consumo de energia por período e cadastro Mobile de deslocamentos.
- Catálogo de fatores com unidade, fonte, versão, validade, escopo, região e premissas; manter histórico dos fatores usados.
- Cálculo no backend e histórico de registros válidos, pendentes ou substituídos.
- Dashboard Web com emissões monitoradas, cobertura, composição por fonte, evolução e comparação somente entre períodos comparáveis.
- Metas de redução com período-base e prazo, mais ações com responsável, prazo e status.
- Persistência PostgreSQL, validações, tratamento de erros, testes e documentação.

Escopo detalhado e critérios: [`docs/planejamento/escopo-mvp.md`](docs/planejamento/escopo-mvp.md). O plano de seis semanas é uma proposta sujeita à capacidade da equipe e validação acadêmica.

## Requisitos e decisões de escopo

Os requisitos funcionais da versão 2.0 usam a identificação **RF-v2-01 a RF-v2-09**, para não sobrescrever a baseline original RF01–RF11. A correspondência e os itens adiados/retirados estão em [`docs/requisitos/requisitos-funcionais.md`](docs/requisitos/requisitos-funcionais.md). A mudança foi registrada em [`docs/decisoes.md`](docs/decisoes.md), e o relatório está em [`docs/planejamento/relatorio-reformulacao-acorde-2.0.md`](docs/planejamento/relatorio-reformulacao-acorde-2.0.md).

## Stack e arquitetura preservadas

| Camada | Tecnologia |
|---|---|
| Web | React + TypeScript + Vite |
| Mobile | Flutter (Android/iOS) |
| Backend e API | Java + Spring Boot; REST HTTP/JSON |
| Dados e segurança | PostgreSQL; Spring Security + JWT |
| Organização | Monólito modular, camadas e referência MVC |

Web e Mobile consomem a mesma API. A organização proposta agrupa por domínio (`auth`, `organizacoes`, `carbono`, `fatores`, `metas`, `acoes`, `dashboard`) e mantém Controllers, Services e Repositories dentro de cada módulo.

## Identidade visual e requisitos não funcionais

A identidade visual oficial Acorde permanece: Verde Profundo `#0F3D34`, Verde Médio `#1F6B58`, Menta `#7FD8B0`, Verde Claro `#E8F6F0`, Off-white `#F7FAF8`, Grafite `#1F2A28`, Cinza `#66746D` e Borda `#DDEAE4`. Usar os SVGs e tokens oficiais do Brand Kit; não redesenhar o monograma. Títulos usam Manrope e interfaces usam Inter. Ver [`GUIA_DA_MARCA.md`](assets/branding/Acorde_Brand_Kit/documentacao/GUIA_DA_MARCA.md).

Os RNFs documentados permanecem: desempenho, acessibilidade WCAG/eMAG, responsividade, Android/iOS, Green IT, segurança e LGPD. Senhas devem usar hash apropriado. Ver [`requisitos não funcionais`](docs/requisitos/requisitos-nao-funcionais.md).

## Estrutura do repositório

```text
.
├── README.md
├── assets/branding/Acorde_Brand_Kit/
└── docs/
    ├── arquitetura/
    ├── der/
    ├── planejamento/
    ├── prototipos/
    ├── requisitos/
    ├── sprints/
    └── testes/
```

Os diretórios `backend/`, `frontend/` e `mobile/` ainda precisam ser criados quando a implementação começar. O estado dos protótipos e da validação de campo permanece pendente; dados de demonstração devem ser identificados como fictícios.

## Referências principais

- [Arquitetura](docs/arquitetura/arquitetura.md)
- [DER da versão 2.0](docs/der/DER_Acorde.md)
- [Validação do problema](docs/planejamento/validacao-problema.md)
- [Cronograma](docs/planejamento/cronograma-6-semanas.md)
- [Protótipos](docs/prototipos/README.md)
- [Plano de testes](docs/testes/plano-testes.md)
- [Decisões](docs/decisoes.md)

---

<div align="center">

### Acorde 2.0

**Gestão em sintonia.**

*Identificar emissões, acompanhar resultados e organizar ações de redução.*

**Projeto acadêmico de Desenvolvimento de Sistemas · SENAI · 2026**

</div>
