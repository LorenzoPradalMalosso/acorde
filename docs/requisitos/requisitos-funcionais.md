# Requisitos funcionais — baseline original

> Fonte: documentação inicial do TCC. Esta lista preserva o conteúdo funcional essencial dos 11 requisitos; priorização de MVP disponível em `../planejamento/escopo-mvp.md`. Não tratar como lista de entregas já implementadas.

| Código | Nome | Necessidade estabelecida |
| --- | --- | --- |
| RF01 | Calendário Corporativo Inteligente | Visões diária/semanal/mensal, eventos, prazos e reuniões; incentivo a modalidades remotas |
| RF02 | Painel de Lembretes em Destaque | Mural de post-its, notas rápidas e avisos prioritários |
| RF03 | Central de Notificações e Alertas | Alertas em tempo real no app e no navegador sobre eventos e prazos |
| RF04 | Controle de Permissões de Agendamento | Gestores gerenciam eventos gerais; colaboradores dependem de permissão ou aprovação |
| RF05 | Cadastro e Validação Anti-Duplicidade de Tarefas | Usuários autenticados cadastram tarefas e recebem alerta de possíveis duplicidades |
| RF06 | Vinculação de Impacto Ecológico por Tarefa | Tags verdes associadas a tarefas/reuniões e estimativa de papel ou CO₂ |
| RF07 | Registro e Acompanhamento de Metas de Sustentabilidade | Cadastro, progresso e visualização gráfica de metas ESG |
| RF08 | Recompensas e Gamificação ESG | Pontuações, insígnias e conquistas por entregas e ações sustentáveis |
| RF09 | Indicador de Carga de Trabalho | Percentual de utilização e alerta de sobrecarga para gestor |
| RF10 | Dashboards Integrados (Operacional + ESG) | Indicadores de entregas no prazo e sustentabilidade |
| RF11 | Autenticação e Controle de Acesso | Login por e-mail/senha e perfis Gestor/Colaborador |

## Regra RF09

`U = (He/Hc) × 100`, onde `He` = horas estimadas atribuídas e `Hc` = horas disponíveis no período.

Faixas de interface: verde até 85%, amarelo de 86% a 99% e vermelho a partir de 100%. Critérios para valores intermediários e `Hc=0` ainda devem ser especificados.

## Critérios de aceitação

Cada RF deverá ter critérios testáveis vinculados às respectivas issues do GitHub. Quando houver diferença entre requisito original e MVP, registrar aprovação em `../decisoes.md`.
