# Requisitos funcionais — Acorde 2.0

> **Versão do sistema: 2.0.** Este catálogo é a proposta funcional da reformulação de carbono. A baseline acadêmica original RF01–RF11 permanece preservada na seção de rastreabilidade; os novos IDs não a sobrescrevem. Necessidade, piloto, fatores e recorte ainda dependem de validação. Nenhum requisito é evidência de funcionalidade implementada.

## Catálogo RF-v2

| Código | Requisito | Resultado esperado no MVP |
|---|---|---|
| RF-v2-01 | Autenticação e perfis | Login por e-mail/senha e autorização de gestor/colaborador no backend |
| RF-v2-02 | Organizações e membros | Vínculo de usuários a organizações; leituras e alterações isoladas por organização |
| RF-v2-03 | Registro de atividades emissoras | Registro de consumo de energia pela Web e deslocamento casa–trabalho pelo Mobile, com quantidade, unidade e período |
| RF-v2-04 | Fatores de emissão | Cadastro/seleção versionada de fatores com fonte, escopo, unidade, região, validade e premissas |
| RF-v2-05 | Cálculo rastreável | Calcular kg CO₂e no backend quando atividade e fator forem compatíveis; reter fator e valores aplicados |
| RF-v2-06 | Dashboard e histórico | Exibir total monitorado parcial, registros por fonte, período, evolução e qualidade/cobertura dos dados |
| RF-v2-07 | Metas de redução | Definir categoria, período-base, valor-base, percentual-alvo, prazo e estado |
| RF-v2-08 | Planos de ação | Criar ações ligadas a uma meta, atribuir responsável, prazo e estado; atualizar andamento autorizado |
| RF-v2-09 | Indicadores ambientais | Apresentar comparações somente com cobertura e metodologia comparáveis; sinalizar dados faltantes e variações |

## Regras de cálculo e apresentação

```text
E = Q × FE

E  = emissão estimada em kg CO₂e
Q  = quantidade da atividade em unidade definida
FE = fator de emissão compatível com a unidade, categoria, escopo, região e período
```

- Não calcular se a quantidade, unidade ou fator adequado estiver ausente; manter estado pendente, sem gerar zero artificial.
- Guardar o identificador e a versão do fator, sua fonte e validade, além da quantidade, unidade, fator e resultado usados em cada cálculo.
- Alteração da atividade ou do método gera revisão rastreável; somente o cálculo vigente entra no total, sem apagar histórico nem somar revisões em duplicidade.
- Diferenciar emissão monitorada, cenário hipotético, emissão evitada e atividade/ação concluída.
- O painel deve dizer que o total cobre as fontes e períodos registrados, sem chamar a soma parcial de inventário completo.
- Comparar redução apenas quando período-base e período atual forem comparáveis. Se a base for zero ou inválida, a redução percentual permanece indefinida; uma variação negativa pode indicar aumento de emissões.
- Emissões diretas operacionais iguais a zero para caminhada/bicicleta não significam impacto de ciclo de vida zero.
- Evitar dupla contagem de caronas e registros repetidos; a regra de deduplicação ainda deve ser especificada.

## Baseline original preservada (RF01–RF11)

| Código | Requisito original | Necessidade estabelecida na especificação inicial |
|---|---|---|
| RF01 | Calendário Corporativo Inteligente | Visões diária/semanal/mensal, eventos, prazos e reuniões; incentivo a modalidades remotas |
| RF02 | Painel de Lembretes em Destaque | Mural de post-its, notas rápidas e avisos prioritários |
| RF03 | Central de Notificações e Alertas | Alertas em tempo real no app e navegador sobre eventos e prazos |
| RF04 | Controle de Permissões de Agendamento | Gestores gerenciam eventos gerais; colaboradores dependem de permissão ou aprovação |
| RF05 | Cadastro e Validação Anti-Duplicidade de Tarefas | Usuários autenticados cadastram tarefas e recebem alerta de possíveis duplicidades |
| RF06 | Vinculação de Impacto Ecológico por Tarefa | Tags verdes em tarefas/reuniões e estimativa de papel ou CO₂ |
| RF07 | Registro e Acompanhamento de Metas de Sustentabilidade | Cadastro, progresso e visualização gráfica de metas ESG |
| RF08 | Recompensas e Gamificação ESG | Pontuações, insígnias e conquistas por entregas e ações sustentáveis |
| RF09 | Indicador de Carga de Trabalho | Percentual de utilização e alerta de sobrecarga para gestor |
| RF10 | Dashboards Integrados (Operacional + ESG) | Indicadores de entregas no prazo e sustentabilidade |
| RF11 | Autenticação e Controle de Acesso | Login por e-mail/senha e perfis Gestor/Colaborador |

Regra original RF09: `U = (He/Hc) × 100`, em que `He` é a soma das horas estimadas atribuídas e `Hc` a capacidade disponível no período. Faixas: verde até 85%, amarelo de 86% a 99%, vermelho a partir de 100%. Arredondamento, indisponibilidade e `Hc = 0` ainda não foram especificados.

## Correspondência com RF original

| Baseline original | Tratamento proposto na versão 2.0 |
|---|---|
| RF01 Calendário corporativo | Adiado; calendário genérico fora do MVP |
| RF02 Mural de lembretes | Adiado |
| RF03 Notificações e alertas | Alertas ambientais podem ser expansão; push não entra no MVP |
| RF04 Permissões de agendamento | Aprovação de agenda adiada; autorização de registros passa ao RF-v2-01/02 |
| RF05 Cadastro/anti-duplicidade de tarefas | Gestão genérica de tarefas sai do núcleo; ações de redução são cobertas por RF-v2-08. Deduplicação das atividades emissoras continua necessária |
| RF06 Impacto ecológico em tarefas/reuniões | Substituído no produto 2.0 por fontes de emissões e cálculo rastreável RF-v2-03/04/05 |
| RF07 Metas ESG | Recortado para metas de redução de carbono RF-v2-07 |
| RF08 Gamificação ESG | Adiado, fora do MVP |
| RF09 Carga de trabalho | Retirado do novo escopo; manter na baseline acadêmica até aprovação formal |
| RF10 Dashboard operacional + ESG | Reposicionado para histórico e dashboard ambiental RF-v2-06/09; indicador de ações permanece separado |
| RF11 Autenticação e acesso | Preservado e detalhado em RF-v2-01/02 |

## Requisitos não funcionais

Os compromissos `RNF01–RNF06` permanecem ativos e detalhados em [`requisitos-nao-funcionais.md`](requisitos-nao-funcionais.md). Segurança e isolamento organizacional aplicam-se também a registros ambientais e históricos de cálculo.

## Critérios de aceitação iniciais

Cada requisito deve ser ligado a issue, critérios objetivos, testes e evidências. Antes de aceitar resultados ambientais, deve ser possível rastrear atividade, fator, unidade, versão e período. Dados de demonstração fictícios devem estar identificados. A matriz RBAC, estados e contrato de API continuam pendentes de especificação.
