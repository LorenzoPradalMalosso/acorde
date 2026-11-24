# Evidências de implementação — Fase 04 Web

**Versão:** Acorde 2.0 · **Estado:** interface implementada e build/lint aprovados; validação visual em navegador real permanece pendente.

## Implementado

- Login do gestor por e-mail/senha e seleção de organização quando a API responde `organization_selection_required`; colaboradores são encaminhados para o Mobile.
- Sessão mantida somente em memória, logout remoto, expiração automática e feedback para falha de rede.
- Navegação para visão geral, registro de energia e histórico; metas/ações aparecem como indisponíveis até suas fases.
- Cadastro e listagem consomem `POST /api/carbon/activities/energy` e `GET /api/carbon/activities` sem dados demonstrativos locais.
- Formulário valida consumo positivo, período e região; mantém a chave de idempotência entre tentativas da mesma submissão e a substitui quando os dados mudam.
- Estados de carregamento, vazio, erro e pendência. Resultado de emissão somente é exibido quando a API retorna valor; não há cálculo no cliente.
- Brand Kit oficial importado como CSS tokens e logo, layout responsivo e modo claro/escuro.

## Verificações executadas

- `npm run lint` — passou com Oxlint, sem warnings.
- `npm run build` — passou (TypeScript e Vite).
- `git diff --check` — sem erros de whitespace nos arquivos alterados; permanecem avisos Git de conversão LF/CRLF em arquivos previamente modificados.

## Pendências de validação

- A API V3 e persistência têm cobertura automatizada em PostgreSQL conforme [`evidencias-fase-03.md`](evidencias-fase-03.md), mas o formulário Web ainda não foi exercitado até o banco por navegador.
- Não foi executado teste E2E de teclado/leitor de tela em navegador real. A tentativa de iniciar Edge headless com protocolo de depuração foi rejeitada pela política automática de execução; o ambiente não disponibiliza ferramenta de desktop/navegador compartilhado neste turno. Os controles usam elementos HTML nativos, rótulos e foco visível; isso não substitui auditoria manual.
- O protótipo PNG em `docs/prototipos` permanece referência não confirmada. Foram reutilizados apenas elementos compatíveis com o Brand Kit e com o escopo implementado.
- Indicadores mostrados são contagens/somas de registros da API; não são o dashboard ambiental completo da Fase 6.
