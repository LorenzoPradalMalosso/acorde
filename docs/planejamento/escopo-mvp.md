# Escopo do MVP — Acorde 2.0

> **Versão do sistema: 2.0.** O escopo abaixo segue a proposta de monitoramento e redução da pegada de carbono operacional. É o recorte de desenvolvimento proposto; a validação acadêmica, o piloto e a metodologia ainda estão pendentes. Não representa funcionalidades implementadas.

## Objetivo

Demonstrar uma jornada integrada que registra duas fontes selecionadas de emissões, calcula estimativas rastreáveis em kg CO₂e, exibe dados parciais e comparáveis e conecta metas a ações com responsáveis.

## Fluxo demonstrável

1. Gestor autentica-se na Web e registra consumo de energia elétrica por período.
2. Colaborador autentica-se no Mobile ou na Web e registra o próprio deslocamento casa–trabalho com modalidade, distância, período/dias e ocupação quando o fator exigir.
3. API valida organização, perfil, unidade e fator compatível e calcula somente com dados suficientes.
4. Backend persiste atividade e registro de emissão versionado; ausência de fator mantém cálculo pendente.
5. Dashboard Web mostra emissões monitoradas por fonte/período, cobertura e histórico, sem se declarar inventário completo.
6. Gestor define meta de redução e atribui ação; colaborador autorizado atualiza seu estado.
7. Web reflete registros e progresso da ação. A ação concluída não é contabilizada automaticamente como redução.

## Entregas mínimas

| Entrega | Critério de aceite proposto | Requisitos |
|---|---|---|
| Autenticação e organizações | Login válido/inválido, perfil reconhecido e dados isolados por organização | RF-v2-01/02 |
| Energia | Gestor registra kWh, período e localização/rede necessária ao fator; registro persiste | RF-v2-03 |
| Mobilidade | Colaborador registra deslocamento sem GPS automático; unidades e dados necessários validados | RF-v2-03 |
| Fatores | Fator tem fonte, versão, validade, região, escopo, unidades e premissas; histórico usado pelo cálculo é preservado | RF-v2-04 |
| Cálculo | Backend calcula `E = Q × FE` com unidade compatível; fator ausente/incompatível não gera zero | RF-v2-05 |
| Histórico e dashboard | Exibe emissões parciais por fonte/período, dados ausentes e variação apenas entre períodos comparáveis | RF-v2-06/09 |
| Metas e ações | Gestor cria meta com base/prazo e ação atribuída; atualização autorizada reflete no Web | RF-v2-07/08 |
| Qualidade | Persistência PostgreSQL, validações, erros tratados, testes e documentação | RNF01–06 |

## Prioridade de execução

Entregar primeiro autenticação, isolamento organizacional, persistência e cálculo no backend. Em seguida, concluir os dois registros de fonte e a integração Web/Mobile. Só então concluir dashboard, metas e ações. Se o prazo ficar em risco, reduzir primeiro a abrangência dos fatores ou das metas junto à orientação acadêmica; não simular cálculos ou integração para aparentar completude.

## Fora do MVP

Inventário corporativo completo; garantias/certificações; compensações e créditos de carbono; integração automática com concessionárias; GPS automático; inteligência artificial; frota própria, viagens corporativas, papel, materiais e resíduos; calendário genérico; mural; notificações push; gamificação avançada; carga de trabalho; análises semânticas.

## Restrições e dependências

- Identificar organização piloto e validar a necessidade em campo.
- Pesquisar e aprovar fontes/fatores reais e seus períodos, regiões e metodologias antes de usá-los como dados de produto.
- Especificar método de deslocamento, unidade (passageiro-km ou veículo-km), ocupação, dias/viagens e deduplicação de caronas.
- Definir cobertura mínima, estado dos registros/cálculos, RBAC e contratos REST.
- Escolher Flyway ou Liquibase antes de gerar migrações.
- Exemplos fictícios do material de contexto são apenas ilustrativos; dados de teste devem ser explicitamente rotulados.
- Validar o recorte com o professor; requisitos acadêmicos originais continuam rastreados em `requisitos-funcionais.md`.
