# Iniciar o Acorde 2.0 para testes

Este tutorial prepara o ambiente local e inicia API, Web e Mobile do MVP Acorde 2.0. O perfil `local-demo` carrega contas fictícias e fatores sintéticos identificados como dados de teste, somente para demonstração local.

## Pré-requisitos

- Windows 10/11 e PowerShell
- Git
- Java 21
- Node.js 24 LTS com npm
- Docker Desktop com Docker Compose, ou uma instalação PostgreSQL acessível
- Flutter stable para executar o aplicativo Mobile

## 1. Configurar o ambiente

Na raiz do repositório, crie o arquivo local de configuração:

```powershell
Copy-Item .env.example .env
```

O script da API gera uma chave JWT aleatória e salva somente no `.env`, que é ignorado pelo Git.
O `.env` é a fonte de configuração do Compose e do `run-local.ps1`; o script aplica os valores do arquivo ao processo Java mesmo quando o terminal contém variáveis antigas. Se for usar as contas de demonstração, configure `ACORDE_DEMO_PASSWORD` e siga a seção 4 antes de iniciar a API.

### Opção A: iniciar PostgreSQL pelo Docker Compose

```powershell
docker compose up -d postgres
```

Confira se o contêiner está ativo:

```powershell
docker compose ps
```

O Compose publica o PostgreSQL na porta 5433 por padrão para evitar conflito com instalações locais que usam 5432. Se essa porta também estiver ocupada, ajuste `POSTGRES_PORT` e a porta em `DB_URL` no `.env` para o mesmo valor. `POSTGRES_PORT` é a porta do computador; dentro do container o PostgreSQL continua na 5432.

As variáveis `POSTGRES_*` inicializam o volume somente na primeira criação. Alterá-las depois não troca a senha gravada em um volume existente. Se a conexão falhar, confira se API e Compose usam o mesmo `.env` e se a porta publicada aponta para este container. Não remova o volume para corrigir credenciais: ajuste a senha no banco existente com uma conta administrativa.

### Opção B: usar PostgreSQL já instalado

Edite `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no `.env` com os dados do banco local. A conta precisa poder criar tabelas e índices no banco indicado. O script executa as migrations Flyway na inicialização.

## 2. Iniciar a API

Em um terminal PowerShell na raiz:

```powershell
.\backend\run-local.ps1
```

Para iniciar com contas e fatores sintéticos, use `.\backend\run-local.ps1 -DemoData` conforme a seção 4. Execute apenas uma instância da API. O health check deve responder em <http://localhost:8080/api/health> com status `UP`. Para parar a API, pressione `Ctrl+C` nesse terminal.

## 3. Iniciar o frontend Web

Abra outro terminal na raiz:

```powershell
cd frontend
npm ci
npm run dev
```

Abra o endereço exibido pelo Vite, normalmente <http://localhost:5173>. O Vite encaminha as chamadas `/api` para a API local.

## 4. Carregar contas fictícias de teste

O perfil de demonstração cria duas organizações, cada uma com um gestor e um colaborador, somente se as tabelas de organizações, usuários e membros estiverem vazias. Se o banco já contiver esses dados, não os altera nem acrescenta fatores. Em um banco novo, se o catálogo também estiver vazio, insere dois fatores sintéticos BR-SP, rotulados com fonte `SINTÉTICO` e versão `TESTE-LOCAL-1`. Esses valores permitem percorrer a jornada e não representam fatores ambientais reais; nunca use `local-demo` em produção.

Defina `ACORDE_DEMO_PASSWORD` no `.env` como uma senha local de 12 a 72 bytes UTF-8. Ela será a senha de login das quatro contas fictícias; não é a senha do PostgreSQL. O script valida o tamanho sem exibir o valor. Não adicione essa senha ao Git:

```powershell
notepad .env
```

Preencha `ACORDE_DEMO_PASSWORD` no `.env` e salve. Se iniciou a API sem dados de demonstração na seção 2, encerre-a com `Ctrl+C`; em seguida, inicie-a uma única vez com o perfil de demonstração:

```powershell
.\backend\run-local.ps1 -DemoData
```

Contas disponíveis (todas usam a senha configurada localmente):

| Organização | E-mail | Perfil |
|---|---|---|
| Demonstração A | `gestor-a@acorde.local` | GESTOR |
| Demonstração A | `colaborador-a@acorde.local` | COLABORADOR |
| Demonstração B | `gestor-b@acorde.local` | GESTOR |
| Demonstração B | `colaborador-b@acorde.local` | COLABORADOR |

Teste o isolamento entrando como gestor A e consultando a organização B; a API deve negar o acesso. O colaborador não deve conseguir listar ou alterar papéis de membros.

### Conferir os cálculos da demonstração

Use um banco dedicado à demonstração, por exemplo `DB_URL=jdbc:postgresql://localhost:5433/acorde_demo`. Ao iniciar a API, confirme no log o perfil `local-demo` e o nome do banco na linha do Flyway. O perfil semeia os fatores em um banco vazio. Um banco já populado com catálogo vazio continua sem fatores.

1. Na Web, entre como `gestor-a@acorde.local` e registre **100 kWh** em Energia elétrica. Use a região `BR-SP` e um período contido entre 01/01/2020 e 31/12/2030. O resultado sintético esperado é **10 kg CO₂e**.
2. Entre como `colaborador-a@acorde.local` e registre o deslocamento uma vez. Na Web, use **Registrar deslocamento** no menu lateral. No Mobile, use **Registros → Registrar deslocamento**. Informe **20 km** de ida e volta durante **5 dias**, com a mesma região e período. O resultado sintético esperado é **5 kg CO₂e**.
3. Na Web, selecione **Todo o período** no Dashboard e clique em **Atualizar**. Com apenas esses dois registros válidos na organização, o total será **15 kg CO₂e**.

Na Web, abra **Histórico** e selecione **Todos**, **Mobilidade** ou **Energia elétrica**. A opção **Somente pendentes** funciona com qualquer fonte.

Após a disponibilização de um fator compatível, abra **Histórico → Ver revisões → Recalcular estimativa** na Web ou use **Recalcular estimativa** no cartão do registro pendente no Mobile. O botão aparece para quem registrou a atividade e tem o perfil autorizado. O recálculo conserva o registro original e adiciona uma revisão ao histórico. Sem fator compatível para todo o período, o registro segue pendente.

### Acompanhar ações atribuídas

Na Web, entre como gestor e abra **Metas e ações → Ações → Nova ação**. Vincule uma meta e escolha o colaborador responsável. Com a conta desse colaborador, abra **Minhas ações** na Web e use **Iniciar** e depois **Concluir**. A ação fica na aba **Concluídas**. O gestor pode consultar o andamento em **Metas e ações**.

## 5. Iniciar o Mobile Flutter

Em outro terminal:

```powershell
cd mobile
flutter pub get
flutter run
```

O Android Emulator usa `http://10.0.2.2:8080`; o iOS Simulator usa `http://localhost:8080`. Para um dispositivo físico, informe o endereço do computador:

```powershell
flutter run --dart-define=ACORDE_API_URL=http://192.168.1.10:8080
```

Troque o endereço pelo IP local correto. O dispositivo e o computador precisam estar na mesma rede. HTTP está liberado apenas para desenvolvimento local; ambientes publicados devem usar HTTPS.

Na barra superior do app, o botão de tema alterna entre claro e escuro. O tema é mantido enquanto o app está aberto.

## 6. Executar as verificações

Suíte backend (Docker é necessário; o script cria um PostgreSQL efêmero sem volume, aplica migrations e o remove ao terminar):

```powershell
.\backend\scripts\test-postgres.ps1
```

Frontend (lint e build):

```powershell
cd frontend
npm ci
npm run lint
npm run build
```

Mobile e build APK Android:

```powershell
cd mobile
flutter analyze --no-pub
flutter test --no-pub
flutter build apk --no-pub
```

Sem as variáveis de keystore de distribuição, o APK de release usa assinatura debug e serve apenas para demonstração local. Consulte [`mobile/README.md`](mobile/README.md) para configurar assinatura externa sem versionar segredos.

## Encerrar

Pare API, Vite e Flutter com `Ctrl+C` nos respectivos terminais. Para parar o PostgreSQL iniciado por Compose, mantendo os dados do volume:

```powershell
docker compose stop postgres
```

Remova o `.env` local somente se não precisar mais das configurações; ele contém a chave JWT de desenvolvimento. Nunca use a configuração ou as contas de demonstração em produção.

Veja também o [contrato da API](docs/api/autenticacao-organizacoes.md), o [plano de implementação](docs/planejamento/plano-implementacao.md), o [relatório final com checklist e pendências](docs/testes/relatorio-final-entrega.md), as [evidências de testes das Fases 2](docs/testes/evidencias-fase-02.md) e [8–9](docs/testes/evidencias-fases-08-09.md), e as [orientações de implantação](docs/implantacao-preparacao.md).
