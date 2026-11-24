# Plano de testes inicial — Acorde 2.0

> **Versão do sistema: 2.0.** Template de planejamento. Nenhum teste está marcado como executado e não existe aplicação implementada neste momento.

## Testes prioritários do MVP

| ID | Cenário | Resultado esperado | Estado |
|---|---|---|---|
| T-01 | Login válido | Acesso autenticado conforme perfil e organização | Não executado |
| T-02 | Login inválido | Mensagem controlada e acesso negado | Não executado |
| T-03 | Colaborador tenta operação exclusiva do gestor | API rejeita sem depender da interface | Não executado |
| T-04 | Usuário tenta consultar/alterar dado de outra organização | API nega; nenhum dado é exposto | Não executado |
| T-05 | Gestor registra consumo de energia válido | Atividade e período persistem com organização correta | Não executado |
| T-06 | Colaborador registra deslocamento válido no Mobile | Atividade persiste e aparece no histórico autorizado | Não executado |
| T-07 | Quantidade negativa, unidade incompatível ou dados obrigatórios ausentes | Validação rejeita ou mantém pendente; nenhum cálculo inválido é exibido | Não executado |
| T-08 | Não existe fator aplicável à categoria/região/período | Estado pendente, nunca zero artificial | Não executado |
| T-09 | Cálculo com fator de teste conhecido | `E = Q × FE` resulta em valor e unidade esperados | Não executado |
| T-10 | Fator revisado após cálculo | Histórico preserva fator e resultado anteriores; novo cálculo é versionado | Não executado |
| T-11 | Edição/revisão da atividade | Somente registro vigente entra no total; sem duplicação | Não executado |
| T-12 | Registro duplicado de deslocamento/caronas | Regra de duplicidade evita dupla contagem conforme método aprovado | Não executado |
| T-13 | Dashboard sem cobertura completa | Total identificado como parcial e lacunas sinalizadas | Não executado |
| T-14 | Comparação de períodos não equivalentes | Sistema não apresenta redução enganosa; informa comparação indisponível | Não executado |
| T-15 | Base de meta ausente ou igual a zero | Percentual de redução fica indefinido; sem divisão por zero | Não executado |
| T-16 | Ação concluída | Estado operacional atualiza; conclusão não altera emissões sem novo dado | Não executado |
| T-17 | Falha de rede/API | Interface informa erro e permite recuperação sem duplicar envio | Não executado |
| T-18 | Uso por teclado, leitor de tela e contraste | Fluxos principais têm operação e feedback acessíveis | Não executado |

## Método e dados de teste

Separar fatores fictícios de testes automatizados e fatores reais aprovados para uso. Identificar qualquer dado de demonstração como fictício. Cobrir unidades, fronteiras de validade temporal/regional, precisão decimal, estados pendente/vigente/substituído, isolamento entre organizações e transições permitidas.

O teste unitário de cálculo deve usar valores conhecidos e verificar a unidade. A atualização do catálogo de fatores não pode alterar silenciosamente cálculos históricos. Comparações devem exigir metodologia e cobertura compatíveis.

## Medição de desempenho RNF01

Definir dispositivo, rede, ambiente, volume de dados, operação medida e procedimento repetível. Registrar tempo e evidência para consultas, gravações e dashboard. Não afirmar conformidade sem medições.

## Acessibilidade e segurança

Verificar WCAG 2.1/2.2 A/AA e eMAG aplicáveis, navegação por teclado, rótulos, feedback de erros, contraste, leitor de tela quando possível, autorização no backend, isolamento de organização, minimização de dados e ausência de segredos em logs.

## Registro de execução

| Teste | Data | Ambiente | Resultado | Evidência | Responsável |
|---|---|---|---|---|---|
| A preencher | — | — | — | — | — |
