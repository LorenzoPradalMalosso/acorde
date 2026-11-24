# Instruções permanentes para agentes — Acorde 2.0

## Contexto do produto

- O produto documentado é o **Acorde 2.0**, voltado a monitorar fontes selecionadas da pegada de carbono operacional e apoiar ações de redução.
- O MVP documentado contempla energia elétrica e deslocamento casa–trabalho, estimativas rastreáveis em kg CO₂e, histórico/dashboard, metas e ações.
- O repositório contém a base técnica, autenticação/organizações (V2), motor de carbono (V3), clientes Web/Mobile, dashboard e metas/ações (V4). Consulte as evidências por fase e valide o estado do Git antes de descrever funcionalidades como aceitas; integrações visuais em navegador/dispositivo e alguns gates acadêmicos/operacionais continuam pendentes.
- Escopo e requisitos funcionais estão em [`docs/planejamento/escopo-mvp.md`](docs/planejamento/escopo-mvp.md) e [`docs/requisitos/requisitos-funcionais.md`](docs/requisitos/requisitos-funcionais.md). Consulte também [`docs/planejamento/plano-implementacao.md`](docs/planejamento/plano-implementacao.md), que organiza a execução em fases 0–9.

## Decisões e autoridade

- Preserve as decisões D-001 e D-002: React + TypeScript + Vite; Flutter; Java + Spring Boot; REST HTTP/JSON; PostgreSQL; Spring Security + JWT; monólito modular, arquitetura em camadas e referência MVC.
- Preserve a identidade visual oficial Acorde. Não redesenhe o monograma, altere os logotipos ou troque a paleta sem autorização explícita.
- D-003 autoriza orientar o projeto pelo posicionamento Acorde 2.0; validação acadêmica do recorte permanece pendente. Os itens `P-*` em `docs/decisoes.md` são propostas/bloqueios, não decisões aprovadas. D-012 descreve regras provisórias de metas/ações e RBAC: preserve a ressalva até aprovação em P-004.
- RF-v2-01 a RF-v2-09 são os IDs do catálogo novo. Mantenha RF01–RF11 como baseline rastreável; não os apague nem reutilize seus IDs para requisitos diferentes.
- Não adicionar funcionalidades fora do escopo do MVP sem aprovação e registro em `docs/decisoes.md`.

## Regras de domínio

- Calcule `E = Q × FE` apenas com quantidade, unidade, categoria, período e fator compatíveis.
- Não invente fatores reais, fontes, resultados de piloto, testes ou dados ambientais. Exemplos sintéticos devem ser claramente identificados como dados de teste.
- Fator ausente/incompatível ou dados insuficientes produzem estado pendente, nunca emissão zero artificial.
- Preserve fonte, versão, unidade, escopo, validade, região e premissas do fator utilizado em cada cálculo; não altere silenciosamente o histórico.
- Totais incluem somente registros válidos vigentes. Não somar revisões substituídas nem contar duas vezes atividade/caronas.
- Informe cobertura e natureza parcial das fontes. Não chame o resultado de inventário corporativo completo.
- Não confunda atividade/ação concluída com redução ou emissão evitada. Compare períodos apenas quando metodologia e cobertura forem compatíveis.
- Minimize dados pessoais. Não implemente coleta de endereço ou GPS automático sem necessidade validada.
- Aplique autenticação, autorização e isolamento organizacional no backend; controles visuais não substituem autorização na API.

## Arquitetura e código

- Organize o backend por módulos de domínio (`auth`, `organizacoes`, `carbono`, `fatores`, `calculos`, `metas`, `acoes`, `dashboard`), com Controllers, Services e Repositories internos ao módulo.
- Mantenha Web e Mobile como clientes da mesma API. Regras de cálculo, autorização e agregação ficam no backend.
- Não introduza microsserviços ou uma nova stack como decisão unilateral.
- Use migrations SQL versionadas pelo Flyway; não crie schema implícito sem migração documentada. V4 implementa metas/ações conforme proposta provisória D-012; tabelas/regra novas que extrapolem essa proposta ou o escopo do MVP dependem de aprovação e registro.
- Nunca grave senha em texto puro; use hash apropriado para senhas. Não comite segredos, tokens ou dados pessoais de usuários.
- Não adicione dependências ou serviços externos sem justificar a necessidade e registrar impactos de manutenção.

## Identidade visual e acessibilidade

- Consulte [`assets/branding/Acorde_Brand_Kit/documentacao/GUIA_DA_MARCA.md`](assets/branding/Acorde_Brand_Kit/documentacao/GUIA_DA_MARCA.md) antes de construir interfaces.
- Reutilize `tokens/acorde.css`, `tokens/acorde_colors.dart`, logotipos e ícones oficiais; não codifique novas tonalidades de marca localmente.
- Paleta base: Verde Profundo `#0F3D34`, Verde Médio `#1F6B58`, Menta `#7FD8B0`, Verde Claro `#E8F6F0`, Off-white `#F7FAF8`, Grafite `#1F2A28`, Cinza `#66746D`, Borda `#DDEAE4`.
- Títulos: Manrope; interface/textos: Inter. Os arquivos de fonte não estão incluídos; respeite licença e carregamento definido para cada plataforma.
- Não use Menta como texto sobre branco sem conferir contraste. Cores de erro, alerta e sucesso são funcionais e distintas das cores de marca.
- Considere WCAG 2.1/2.2 A/AA e eMAG, teclado, leitores de tela, rótulos e feedback de erro desde a primeira interface.

## Documentação e processo

- Leia este arquivo e os documentos de escopo/requisitos/arquitetura/dados relacionados antes de alterar uma área.
- Mantenha documentação, implementação, protótipos e plano de testes coerentes. Registre mudanças aprovadas, justificativa, requisitos afetados e autoria em `docs/decisoes.md`/relatórios de sprint.
- O histórico sobre a ferramenta de prototipação é conflitante (Figma ou Google Stitch); trate plataforma e artefatos como não confirmados até validação da equipe.
- Não marque critérios como concluídos nem afirme execução de testes sem evidência reproduzível. Ao implementar, siga os critérios de aceite do plano e da issue aplicável.
- Trabalhe incrementalmente e valide cada implementação. Ao finalizar cada fase, atualize o plano, decisões, testes, evidências e pendências; não espere fases posteriores para validar fatias independentes.
- Utilize skills, Context7 e documentação oficial quando necessário e disponível. Prefira documentação primária para dependências, ferramentas e APIs.
- Use branches e PRs revisáveis conforme o fluxo acadêmico documentado. Não faça alterações destrutivas ou restaure alterações locais do usuário sem solicitação.
- Não faça push, merge ou deploy externo sem autorização explícita. Não exponha credenciais nem execute operações destrutivas.
