# Expansões do modelo de dados — Acorde 2.0

Este documento mantém propostas fora do MVP. Não implementar antes de priorizar cada fonte, validar metodologia, regras de autorização e capacidade de suporte. O MVP proposto cobre energia elétrica e deslocamento casa–trabalho.

| Expansão | Dados/entidades a detalhar | Observações |
|---|---|---|
| Combustível de frota própria | detalhe de combustível/veículo vinculado a atividade emissora | Potencial escopo 1; requer tipo/quantidade de combustível, período, unidade e método aprovados |
| Viagens corporativas | trechos, modalidade e distância vinculados a atividade emissora | Definir alocação por passageiro, ida/volta e fatores por modalidade |
| Papel, materiais e resíduos | tipo de material, quantidade, unidade e destino | Não reintroduzir economia presumida por tarefa; emissões evitadas requerem método próprio e permanecem separadas |
| Alertas ambientais | notificações derivadas de metas, dados faltantes ou prazos | Expansão futura; push/tempo real fora do MVP |
| Relatórios avançados | exportação/agregações sobre registros válidos | Manter totais derivados e cobertura/metodologia visíveis |

As novas fontes podem reutilizar `atividades_emissoras`, `fatores_emissao` e `registros_emissao`, com tabelas de detalhe por categoria. Não alterar fatores históricos. Confirmar se esses novos escopos (1, 2, 3) podem ser somados ou comparados e quais limites serão comunicados.

## Integridade e expansão

- Uma atividade recebe somente um tipo de detalhe coerente com sua categoria.
- Cada cálculo guarda a versão exata do fator, unidade e quantidade aplicadas.
- Registros substituídos permanecem para auditoria, mas não compõem o total vigente.
- Materiais e resíduos não devem ser chamados de emissões evitadas sem cenário-base e fator compatíveis.
- Alertas e relatórios não devem duplicar ou materializar total sem estratégia de atualização definida.

O modelo completo editável está em [`DER_Completo.dbml`](DER_Completo.dbml); o diagrama de módulos futuros está em [`DER_Modulos_Futuros.mmd`](DER_Modulos_Futuros.mmd). Ambos são propostas sujeitas a validação. Os arquivos estáticos `DER_Modulos.svg` e `DER_Modulos.png` mostram uma visão resumida das mesmas expansões.
