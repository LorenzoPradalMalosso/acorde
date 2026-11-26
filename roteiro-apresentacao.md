# Acorde 2.0 — Roteiro de apresentação

**Duração sugerida:** 5 a 7 minutos. Para a demonstração, prepare o ambiente local seguindo o [guia de início](iniciar.md).

## 1. Abertura

> O Acorde 2.0 ajuda organizações a acompanhar as emissões associadas ao consumo de energia elétrica e ao deslocamento casa–trabalho. O gestor acompanha os resultados, cria metas e atribui ações. O colaborador registra seus deslocamentos e atualiza as ações que recebeu.

## 2. Tecnologias por função

| Função | Tecnologias | O que dizer |
|---|---|---|
| Interface Web | **React, TypeScript, CSS e Vite** | React monta as telas com componentes reutilizáveis. TypeScript ajuda a conferir os tipos de dados que as telas usam. CSS aplica o visual, e Vite executa e empacota a Web. |
| Aplicativo Mobile | **Flutter e Dart** | O app oferece ao colaborador registro de deslocamento, histórico pessoal e ações atribuídas. Flutter permite usar o mesmo código de interface para Android e iOS. |
| Login e permissões | **Spring Security, JWT e BCrypt** | BCrypt gera o hash da senha. Após o login, o cliente envia um token JWT nas chamadas. O servidor confere a sessão, a organização e o papel antes de autorizar uma operação. |
| Cadastro de energia e deslocamento | **API REST em Java e Spring Boot, com HTTP e JSON** | Web e Mobile usam a mesma API. O gestor informa o consumo em kWh na Web. O colaborador registra o próprio deslocamento na Web ou no app. O servidor valida e salva cada registro. |
| Fatores, cálculo e revisões | **Java `BigDecimal` e PostgreSQL** | A API procura um fator compatível com a fonte, a unidade, a região e o período. Calcula quantidade × fator com precisão decimal e guarda a fonte e a versão usadas. Sem fator compatível, o registro fica pendente. |
| Dashboard e histórico | **Consultas SQL no backend, React e SVG na Web** | O servidor soma os cálculos válidos vigentes e conta as pendências separadamente. A Web exibe os resultados em cartões, gráficos e tabelas. No histórico, dá para filtrar por mobilidade ou energia elétrica e abrir as revisões. |
| Metas e ações | **React, Flutter, Spring Boot e PostgreSQL** | O gestor cria metas e atribui ações pela Web. O colaborador acompanha as próprias ações na Web ou no app e muda seu andamento. O servidor valida as transições e grava os estados no banco. |
| Banco e estrutura das tabelas | **PostgreSQL e Flyway** | PostgreSQL guarda organizações, usuários, atividades, cálculos, metas e ações. Flyway aplica os scripts SQL em ordem para que cada ambiente tenha a estrutura esperada. |
| Execução e qualidade | **Docker Compose, Nginx, JUnit, testes Flutter e GitHub Actions** | Docker Compose organiza a execução dos serviços. No empacotamento, Nginx serve a Web e encaminha chamadas para a API. A CI reúne verificações de código, builds e testes do backend e do app. |

> A ideia principal é que a Web e o app chamam a mesma API. Com as permissões e o cálculo no servidor, a regra é aplicada da mesma forma em qualquer tela.

## 3. Demonstração

1. **Gestor → Energia elétrica.** Mostre um registro de 100 kWh. Com o fator de demonstração de 0,10 kg CO₂e por kWh, a API retorna **10 kg CO₂e**.
2. **Colaborador → Registrar deslocamento.** Mostre 20 km de ida e volta por dia durante 5 dias. Isso corresponde a 100 passageiro-km. Com o fator de demonstração de 0,05 kg CO₂e por passageiro-km, a API retorna **5 kg CO₂e**.
3. **Gestor → Dashboard e Histórico.** Mostre o total de **15 kg CO₂e** se a organização tiver apenas esses dois registros válidos. Abra **Ver revisões** para apontar o fator usado. Se os exemplos já estiverem cadastrados, abra os registros existentes.
4. **Gestor → Metas e ações.** Mostre uma meta e uma ação atribuída. Troque para a conta do colaborador, abra **Minhas ações** e avance de **Planejada** para **Em andamento** e depois para **Concluída**. Volte ao perfil do gestor para conferir o novo estado.

## 4. Encerramento

> A apresentação usou fatores sintéticos. Os resultados do dashboard cobrem as fontes monitoradas, e concluir uma ação indica progresso operacional. Para verificar se houve redução de emissões, precisamos de dados e períodos comparáveis.

Material de apoio: [arquitetura](docs/arquitetura/arquitetura.md), [API de cálculos](docs/api/atividades-e-calculos.md) e [API de dashboard, metas e ações](docs/api/dashboard-metas-acoes.md).
