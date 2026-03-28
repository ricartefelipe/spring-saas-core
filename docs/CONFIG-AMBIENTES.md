# Configuração por ambiente — spring-saas-core

Este projeto segue a mesma convenção de ambientes do **Fluxe B2B Suite**. Referência central: **fluxe-b2b-suite/config/env/README.md** (tabela de portas e hosts).

## Contextos

| Contexto | Como rodar | Config |
|----------|------------|--------|
| **local** | Infra no Docker (`docker compose up -d postgres redis rabbitmq`) + app no host | `SPRING_PROFILES_ACTIVE=local`. Valores em `application.yml` (localhost:5435, 6382, 5675) ou em `.env` / `.env.local` se você exportar antes de rodar |
| **docker** | Tudo no Docker Compose | O `docker-compose.yml` injeta `DB_URL`, `REDIS_HOST`, etc. com hostnames `postgres`, `redis`, `rabbitmq` |
| **staging** / **prod** | Railway ou outro host | `SPRING_PROFILES_ACTIVE=staging` ou `prod`; variáveis no painel do provedor |

## Arquivos de configuração

- **`application.yml`** — Defaults para **local** (portas 5435, 6382, 5675 no host).
- **`application-local.yml`** — Profile `local`: Liquibase contexts, auth em modo HS256.
- **`.env.example`** — Variáveis para rodar no **host** (local). Copie para `.env` ou exporte antes de `./mvnw spring-boot:run`.
- **`.env.local`** — (opcional) Overrides locais; se existir, use com `set -a; source .env.local; set +a` antes de rodar o app. Não é lido automaticamente pelo Spring; use para não alterar `.env` quando alternar entre máquina e Docker.

## Portas local (alinhadas ao suite)

Quando a infra sobe com `docker compose up -d postgres redis rabbitmq`:

- Postgres: **localhost:5435** (database `saascore`)
- Redis: **localhost:6382**
- RabbitMQ: **localhost:5675** (AMQP), 15675 (management)
- App: **8080**

Para rodar o app no host com esses valores (sem editar nada):

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Os defaults em `application.yml` já apontam para localhost:5435, 6382, 5675. Se você usar outro compose com portas diferentes, exporte `DB_URL`, `REDIS_HOST`, `REDIS_PORT`, `RABBITMQ_HOST`, `RABBITMQ_PORT` ou crie `.env.local` e faça `source .env.local` antes do comando acima.

## Assistente IA (Admin)

Ver [IA-ASSISTENTE-ADMIN.md](IA-ASSISTENTE-ADMIN.md): variáveis `OPENAI_API_KEY` + `AI_ENABLED`, diagnóstico via `GET /v1/ai/status`, e exemplo local com Docker Compose.

## Referências

- **fluxe-b2b-suite** (repositório da suite): pasta `config/env/README.md` — tabela única de portas (local vs Docker)
- [SUBIR-E-TESTAR-TODOS-PROJETOS.md](SUBIR-E-TESTAR-TODOS-PROJETOS.md) — Ordem de subida e smoke tests
