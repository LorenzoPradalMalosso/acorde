# Escopo do MVP e rastreabilidade

**Objetivo:** construir em seis semanas um fluxo real entre Web, API, PostgreSQL e Mobile, demonstrando gestão de tarefas e Micro-ESG em cenário validado.

## Fluxo demonstrável

1. Gestor autentica-se na aplicação Web.
2. Gestor cria tarefa, define prazo e atribui colaborador.
3. Backend valida autorização e persiste a tarefa.
4. Colaborador autentica-se no app Mobile, consulta sua tarefa e marca conclusão.
5. Quando aplicável, registra-se uma tag verde e dados declarados para estimativa de papel.
6. Gestor visualiza tarefas e estimativas ambientais no dashboard Web.

## Entregas mínimas

| Entrega | Critério de aceitação sugerido |
| --- | --- |
| Autenticação/controle de acesso | Usuário autenticado recebe seu perfil; perfis não acessam operações não autorizadas |
| Gestão de tarefas | CRUD e atribuição são persistidos; prazo, responsável e status são recuperados corretamente |
| App Mobile | Usuário consulta suas tarefas reais pela API e consegue alterar status quando permitido |
| Micro-ESG simples | Tag e quantidade informada são persistidas; cálculo exibe premissas ou informa que não há dados suficientes |
| Dashboard Web | Dados refletem registros realmente armazenados, não valores simulados |
| Testes/documentação | Cenário principal testado com resultados e evidências reproduzíveis |

## Matriz de prioridade

| Requisito | Recorte do MVP | Complementar ou expansão |
| --- | --- | --- |
| RF01 | — | Calendário e sugestão de modalidade remota |
| RF02 | — | Mural de lembretes |
| RF03 | — | Notificações em tempo real |
| RF04 | — | Fluxo de aprovação de agendamentos |
| RF05 | Gestão básica/CRUD de tarefas | Comparação anti-duplicidade |
| RF06 | Tags verdes + papel em tarefas | Reuniões e CO₂ |
| RF07 | — | Metas de sustentabilidade |
| RF08 | — | Eco-Score e gamificação |
| RF09 | — | Carga de trabalho |
| RF10 | Dashboard essencial | Análises avançadas |
| RF11 | Login e perfis | Gestão avançada de usuários e permissões |

## Restrições e premissas

- Trata-se de uma **priorização proposta**, não de exclusão dos requisitos originais.
- O ambiente de validação real, os dados necessários para estimativas ESG e o modelo de organização ainda precisam ser definidos.
- Sempre distinguir *funcionalidade planejada*, *implementada* e *testada*.
- Para atendimento completo do RNF04, verificar compatibilidade Mobile Android/iOS; se a validação prática ficar limitada a uma plataforma por falta de infraestrutura, declarar a limitação e consultar o professor.
