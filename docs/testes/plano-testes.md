# Plano de testes inicial — Acorde 2.0

> **Versão do sistema: 2.0.** Plano incremental. RF-v2-01/02 foram exercitados em PostgreSQL 18 isolado. T-05 a T-12 têm evidências automatizadas em PostgreSQL 17 isolado; fatores utilizados nesses testes são sintéticos. T-13 a T-16 têm cobertura automatizada de API PostgreSQL adicionada na Fase 7; não substitui validação visual dos clientes.

## Testes prioritários do MVP

| ID | Cenário | Resultado esperado | Estado |
|---|---|---|---|
| T-01 | Login válido e perfil/vínculo coerentes | JWT válido, perfil conforme vínculo do banco e leitura limitada à organização | Passou — PostgreSQL 18.6 |
| T-02 | Login inválido ou organização sem vínculo | Resposta genérica 401, sem autenticar nem revelar conta/organização | Passou — PostgreSQL 18.6 |
| T-03 | Colaborador tenta operação exclusiva do gestor | API responde 403; tentativa não depende da interface | Passou — PostgreSQL 18.6 |
| T-04 | Usuário tenta consultar/alterar membro de outra organização | API responde 404; nenhum dado é exposto ou alterado | Passou — PostgreSQL 18.6 |
| T-05 | Gestor registra consumo de energia válido | Atividade e período persistem com organização correta | Passou — PostgreSQL 17 isolado; 20 testes totais |
| T-06 | Colaborador registra deslocamento válido no Mobile | Atividade persiste e aparece no histórico autorizado | Passou na API com conta sintética isolada; fluxo visual em Android/iOS pendente |
| T-07 | Quantidade negativa, unidade incompatível ou dados obrigatórios ausentes | Validação rejeita ou mantém pendente; nenhum cálculo inválido é exibido | Passou — negativo rejeitado; fator incompatível permanece pendente |
| T-08 | Não existe fator aplicável à categoria/região/período | Estado pendente, nunca zero artificial | Passou — PostgreSQL 17 isolado |
| T-09 | Cálculo com fator de teste conhecido | `E = Q × FE` resulta em valor e unidade esperados | Passou — valores decimais sintéticos; precisão persistida |
| T-10 | Fator revisado após cálculo | Histórico preserva fator e resultado anteriores; novo cálculo é versionado | Passou — reprocessamento explícito e snapshot preservado |
| T-11 | Edição/revisão da atividade | Somente registro vigente entra no total; sem duplicação | Passou — revisão explícita cria novo snapshot e substitui o anterior |
| T-12 | Registro duplicado de deslocamento/caronas | Regra de duplicidade evita dupla contagem conforme método aprovado | Parcial — repetição com mesma chave comprovadamente não duplica; chaves diferentes/caronas seguem pendentes conforme D-009 |
| T-13 | Dashboard sem cobertura completa | Total identificado como parcial e lacunas sinalizadas | Passou no endpoint — atividade pendente separada, nota de cobertura e agregação só de válida vigente; revisão Web manual pendente |
| T-14 | Comparação de períodos não equivalentes | Sistema não apresenta redução enganosa; informa comparação indisponível | Parcial — comparação não é calculada nem mostrada; revisão visual ainda pendente |
| T-15 | Base de meta ausente ou igual a zero | Percentual de redução fica indefinido; sem divisão por zero | Passou parcialmente — API aceita base zero e não calcula progresso; teste visual/métrica futura não se aplica ainda |
| T-16 | Ação concluída | Estado operacional atualiza; conclusão não altera emissões sem novo dado | Passou na API — transição/autorização cobertas; cálculo de emissão não é ligado ao estado da ação |
| T-17 | Falha de rede/API | Interface informa erro e permite recuperação sem duplicar envio | Parcial — handling/código de idempotência implementados; teste de falha E2E pendente |
| T-18 | Uso por teclado, leitor de tela e contraste | Fluxos principais têm operação e feedback acessíveis | Não executado — precisa revisão manual nos clientes |

## Método e dados de teste

Separar fatores fictícios de testes automatizados e fatores reais aprovados para uso. Identificar qualquer dado de demonstração como fictício. Cobrir unidades, fronteiras de validade temporal/regional, precisão decimal, estados pendente/vigente/substituído, isolamento entre organizações e transições permitidas.

O teste unitário de cálculo deve usar valores conhecidos e verificar a unidade. A atualização do catálogo de fatores não pode alterar silenciosamente cálculos históricos. Comparações devem exigir metodologia e cobertura compatíveis.

Os testes de autenticação usam quatro usuários fictícios em duas organizações semeados em banco isolado; nenhum dado foi incluído na base PostgreSQL preexistente do ambiente. Consulte [`evidencias-fase-02.md`](evidencias-fase-02.md) para expiração/revogação de token, ausência de autenticação, migrações e validação dos clientes.

## Medição de desempenho RNF01

Definir dispositivo, rede, ambiente, volume de dados, operação medida e procedimento repetível. Registrar tempo e evidência para consultas, gravações e dashboard. Não afirmar conformidade sem medições.

## Acessibilidade e segurança

Verificar WCAG 2.1/2.2 A/AA e eMAG aplicáveis, navegação por teclado, rótulos, feedback de erros, contraste, leitor de tela quando possível, autorização no backend, isolamento de organização, minimização de dados e ausência de segredos em logs.

## Registro de execução

| Teste | Data | Ambiente | Resultado | Evidência | Responsável |
|---|---|---|---|---|---|
| A preencher | — | — | — | — | — |
