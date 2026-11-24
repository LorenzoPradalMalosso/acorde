# Preparação de implantação — Acorde 2.0

**Estado:** imagens Docker Web/API e stack isolado foram construídos e verificados localmente; nenhum provedor, domínio, TLS, backup operacional ou deploy externo foi aprovado/executado.

## Arquitetura preparada

`docker-compose.production.yml` empacota PostgreSQL 17, API Java/Spring e frontend React/nginx. API e PostgreSQL ficam somente na rede interna. A Web também participa de uma rede de borda e publica a porta apenas no loopback do host, para ficar atrás de um proxy/ingress TLS administrado pelo ambiente escolhido. O backend não publica sua porta nem o PostgreSQL.

As migrations Flyway executam na inicialização da API. A aplicação roda como usuário sem privilégios no container. Segredos são recebidos por variáveis de ambiente e o arquivo de exemplo não contém valores secretos. Ainda é necessário decidir como o ambiente real injeta segredos, termina TLS, armazena imagens e observa os serviços.

## Preparação local, sem publicar

1. Copie `.env.production.example` para `.env.production` em host controlado e gere senha PostgreSQL e chave JWT aleatórias (mínimo 32 bytes para a chave). Não commite o arquivo preenchido.
2. Revise a configuração sem iniciá-la: `docker compose --env-file .env.production -f docker-compose.production.yml config`.
3. Faça build local: `docker compose --env-file .env.production -f docker-compose.production.yml build`.
4. Teste o stack somente em ambiente descartável e dados sintéticos. A validação de produção depende de uma decisão de ambiente e de aprovação explícita; esta documentação não executa deploy.

Não use dados de produção em ferramentas locais nem reaproveite o volume local de desenvolvimento.

Na validação local de 24/11/2026, um projeto Compose isolado aplicou migrations V1–V4, inicializou API/PostgreSQL saudáveis e serviu a Web no loopback; o proxy `/api/health` respondeu HTTP 200. O projeto de validação usou banco/volume próprio e senha/chave sintéticas. O build da Web exige contexto na raiz do repositório para incluir o token oficial `assets/branding/Acorde_Brand_Kit/tokens/acorde.css`.

O APK Android compilado sem keystore usa assinatura debug e serve apenas para demonstração local. Para distribuição, configure `ACORDE_ANDROID_KEYSTORE_PATH`, `ACORDE_ANDROID_KEYSTORE_PASSWORD`, `ACORDE_ANDROID_KEY_ALIAS` e `ACORDE_ANDROID_KEY_PASSWORD` no ambiente seguro de release. Nenhuma chave foi criada ou versionada.

## Backup e recuperação — procedimento a validar no ambiente

Antes de qualquer release, definir retenção, criptografia, destino fora do host e responsáveis. Exemplo de backup manual dentro do host escolhido (sem ser executado aqui):

```powershell
docker compose --env-file .env.production -f docker-compose.production.yml exec -T postgres `
  sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > acorde-backup.dump
```

Restauração deve ocorrer primeiro em uma instância isolada e ser validada por checksums/contagens e smoke tests. Para recuperar um ambiente, interromper a aplicação, preservar evidência do estado anterior, restaurar em banco isolado e só então apontar a versão aprovada; nunca apagar volume como mecanismo de rollback.

## Limitações e gates

- Provedor, região, domínio, TLS, gestão de segredos, retenção de logs e política de backup ainda precisam de aprovação.
- Não foi medida disponibilidade, latência, recuperação, segurança de imagem, acessibilidade de release nem custo do host.
- Migrações são forward-only por padrão; rollback exige migration corretiva ou restauração testada em instância isolada.
- O stack ainda requer verificação em ambiente compatível e revisão operacional antes de uso real.
