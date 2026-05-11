# spring-saas-core

[![CI](https://github.com/ricartefelipe/spring-saas-core/actions/workflows/ci.yml/badge.svg)](https://github.com/ricartefelipe/spring-saas-core/actions/workflows/ci.yml)
[![Build & Push](https://github.com/ricartefelipe/spring-saas-core/actions/workflows/build-push.yml/badge.svg)](https://github.com/ricartefelipe/spring-saas-core/actions/workflows/build-push.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-FF6600?logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)](docker-compose.yml)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-6BA539?logo=openapiinitiative&logoColor=white)](docs/api/openapi.yaml)

Núcleo de governança multi-tenant para SaaS: tenants, RBAC/ABAC, feature flags, auditoria e publicação de eventos (outbox). Serve como **control plane** para uma plataforma B2B, integrando com APIs de pedidos (Node) e pagamentos (Python) através de um contrato de identidade JWT unificado.

---

## Índice

- [Visão geral](#visão-geral)
- [Quando usar](#quando-usar)
- [Stack tecnológica](#stack-tecnológica)
- [Quick Start](#quick-start)
- [URLs e serviços](#urls-e-serviços)
- [API REST](#api-rest-prefixo-v1)
- [Autorização (ABAC)](#autorização-abac)
- [Identidade e headers](#identidade-e-headers)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Scripts e automação](#scripts-e-automação)
- [Deploy (GitHub Actions)](#deploy-github-actions)
- [Observabilidade](#observabilidade)
- [Arquitetura do código](#arquitetura-do-código)
- [Testes](#testes)
- [Troubleshooting](#troubleshooting)
- [Demo e vendas](#demo-script-35-minutos)
- [Contratos e documentação](#contratos-de-integração)
- [Licença e autor](#licença)

---

## Visão geral

O **spring-saas-core** é o painel de controle (admin) de uma plataforma SaaS multi-tenant. Ele centraliza:

| Área | Descrição |
|------|-----------|
| **Tenants** | CRUD de organizações com plano (free/pro/enterprise), região e status (ACTIVE, SUSPENDED, DELETED). |
| **Políticas ABAC/RBAC** | Autorização por `permission_code` com filtros por plano e região; regras DENY têm precedência; default-deny. |
| **Feature flags** | Por tenant: on/off, rollout percentual, targeting por roles. |
| **Auditoria** | Todas as ações CRUD e negações (ACCESS_DENIED) registradas para compliance. |
| **Identidade** | Claims JWT padronizadas (`sub`, `tid`, `roles`, `perms`, `plan`, `region`) consumidas por Node e Python. |
| **Outbox** | Eventos de domínio persistidos na mesma transação e publicados via RabbitMQ. |

Outros serviços (node-b2b-orders, py-payments-ledger) **validam** o JWT emitido ou delegado por este núcleo e aplicam as mesmas regras de tenant e permissão.

---

## Quando usar

- Você precisa de um **único ponto de verdade** para tenants, políticas e feature flags.
- Múltiplos backends (Java, Node, Python) devem compartilhar o **mesmo contrato de identidade** (JWT).
- Você quer **auditoria centralizada** e **governança ABAC** (negar por plano/região, não só por role).
- A suíte inclui um frontend (ex.: fluxe-b2b-suite) que consome este core para admin e operações.

---

## Stack tecnológica

| Camada | Tecnologia |
|--------|------------|
| Runtime | Java 21, Spring Boot 3.x (Maven) |
| Segurança | Spring Security (JWT Resource Server), HS256 local ou OIDC em produção |
| Dados | PostgreSQL, Liquibase (YAML) |
| Cache / filas | Redis (opcional), RabbitMQ (opcional) |
| Observabilidade | Micrometer + Prometheus, OpenTelemetry OTLP, MDC (correlationId, tenantId) |
| Documentação | springdoc-openapi, Swagger UI |
| Dashboards | Grafana (ex.: porta 3030) |

---

## Quick Start

### Pré-requisitos

- **Java 21+**, **Maven 3.9+**
- **Docker** e **Docker Compose** (para stack completa e testes de integração)

### Rede Docker compartilhada

Todos os serviços da plataforma Fluxe B2B usam a rede externa `fluxe_shared` para comunicação entre containers. O script `up.sh` cria automaticamente a rede caso ela não exista. Para criar manualmente:

```bash
docker network create fluxe_shared
```

### Opção 1: Docker Compose (recomendado)

Sobe app, PostgreSQL, Redis, RabbitMQ, Prometheus e Grafana:

```bash
./scripts/up.sh
./scripts/seed.sh
./scripts/smoke.sh
```

### Opção 2: Apenas infraestrutura + Maven

```bash
docker compose up -d postgres redis rabbitmq
./mvnw spring-boot:run
```

### Opção 3: Testes unitários (sem Docker)

```bash
./mvnw test -Dtest='!*Integration*'
```

Testes de integração (Testcontainers) exigem Docker em execução.

---

## URLs e serviços

| Serviço | URL | Observação |
|---------|-----|------------|
| Aplicação | http://localhost:8080 | Raiz da API |
| Swagger UI | http://localhost:8080/docs | Documentação interativa |
| OpenAPI JSON | http://localhost:8080/v3/api-docs | Spec para clientes |
| Liveness | http://localhost:8080/actuator/health/liveness | Kubernetes |
| Readiness | http://localhost:8080/actuator/health/readiness | DB e dependências |
| Prometheus | http://localhost:8080/actuator/prometheus | Métricas |
| Grafana | http://localhost:3030 | *(ver docker-compose.yml)* |
| RabbitMQ UI | http://localhost:15672 | *(ver docker-compose.yml)* |

---

## API REST (prefixo /v1)

A API está versionada como **v1** e é estável para integração com node-b2b-orders e py-payments-ledger.

### Tenants

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/v1/tenants` | Criar tenant |
| GET | `/v1/tenants` | Listar (paginado, filtros: status, plan, region, name) |
| GET | `/v1/tenants/{id}` | Obter por ID |
| PATCH | `/v1/tenants/{id}` | Atualizar |
| DELETE | `/v1/tenants/{id}` | Soft delete (status DELETED) |

### Policies (ABAC)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/v1/policies` | Criar política |
| GET | `/v1/policies` | Listar (paginado, filtros) |
| GET | `/v1/policies/{id}` | Obter por ID |
| PATCH | `/v1/policies/{id}` | Atualizar |
| DELETE | `/v1/policies/{id}` | Remover (soft delete) |

### Feature flags (por tenant)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/v1/tenants/{tenantId}/flags` | Criar flag |
| GET | `/v1/tenants/{tenantId}/flags` | Listar flags |
| PATCH | `/v1/tenants/{tenantId}/flags/{name}` | Atualizar flag |
| DELETE | `/v1/tenants/{tenantId}/flags/{name}` | Remover flag |

### Audit log

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/v1/audit` | Consultar (paginado; filtros: tenantId, action, from/to) |
| GET | `/v1/audit/export` | Exportar para compliance: `from`, `to` (obrig.), format=json ou csv, até 10k registros |

### Consumer endpoints (Node/Python)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/v1/tenants/{id}/snapshot` | Snapshot do tenant (id, plan, region, status). Opcional: `?include=policies,flags` |
| GET | `/v1/tenants/{id}/policies` | Políticas aplicáveis ao tenant |
| GET | `/v1/tenants/{id}/flags` | Feature flags do tenant |

### Dev token (profile local)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/v1/dev/token` | Gerar JWT HS256 para testes locais (body: sub, tid, roles, perms, plan, region) |

### Outros

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/v1/me` | Dados do usuário atual (claims + correlation_id) |
| GET | `/healthz` | Liveness |
| GET | `/readyz` | Readiness (DB; Redis/Rabbit opcionais) |

---

## Autorização (ABAC)

1. **JWT** validado (HS256 em local, OIDC em produção).
2. **X-Tenant-Id** deve coincidir com a claim `tid` (exceto para admins globais, conforme políticas).
3. Cada endpoint exige **permission codes** (ex.: `tenants:read`, `tenants:write`).
4. Políticas avaliadas: `enabled=true` e (plan em `allowed_plans` ou vazio) e (region em `allowed_regions` ou vazio).
5. **DENY tem precedência** sobre ALLOW.
6. **Default-deny**: sem política aplicável = acesso negado.
7. Negações são registradas em `audit_log` com action `ACCESS_DENIED`.

---

## Identidade e headers

### Exemplo de claims JWT

```json
{
  "sub": "user@example.com",
  "tid": "uuid-do-tenant",
  "roles": ["admin"],
  "perms": ["tenants:read", "tenants:write"],
  "plan": "enterprise",
  "region": "us-east-1"
}
```

### Headers HTTP

| Header | Obrigatório | Descrição |
|--------|-------------|-----------|
| Authorization | Sim | `Bearer <JWT>` |
| X-Tenant-Id | Sim* | ID do tenant (validado contra claim `tid`) |
| X-Correlation-Id | Não | Se ausente, gerado automaticamente |

---

## Variáveis de ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| SPRING_PROFILES_ACTIVE | local | Profile ativo |
| DB_URL / spring.datasource.url | jdbc:postgresql://... | URL PostgreSQL |
| DB_USER / spring.datasource.username | — | Usuário do banco |
| DB_PASS / spring.datasource.password | — | Senha do banco |
| AUTH_MODE | hs256 | `hs256` (local) ou `oidc` (produção) |
| JWT_ISSUER | spring-saas-core | Issuer do JWT |
| JWT_SECRET / JWT_HS256_SECRET | — | Chave HS256 (gerar com `openssl rand -hex 32`) |
| JWT_SECRET_PREVIOUS / JWT_HS256_SECRET_PREVIOUS | — | Chave anterior para rotação sem downtime |
| OIDC_ISSUER_URI | — | Obrigatório em prod (ex.: Keycloak) |
| REDIS_HOST | localhost | Host Redis |
| RABBITMQ_HOST | localhost | Host RabbitMQ |
| OUTBOX_PUBLISH_ENABLED | false | Habilitar publicação outbox |
| SERVER_PORT | 8080 | Porta do servidor |

---

## Scripts e automação

| Script | Descrição |
|--------|-----------|
| `./scripts/up.sh` | Sobe stack via Docker Compose |
| `./scripts/migrate.sh` | Verifica status das migrações Liquibase |
| `./scripts/seed.sh` | Verifica dados seed (aplicados via Liquibase) |
| `./scripts/smoke.sh` | Smoke tests automatizados (health, CRUD, ABAC, audit) |
| `./scripts/e2e-invite-user.sh` | E2E HTTP: `POST /v1/users/invite` com dev token (requer API em `BASE_URL`) |
| `./scripts/api-export.sh` | Exporta OpenAPI JSON/YAML para `docs/api/` |

---

## Deploy (GitHub Actions)

A publicação da imagem para o **GHCR** é feita pelo workflow **Build & Push Docker Image** (`.github/workflows/build-push.yml`) em push para `develop`/`master` ou execução manual em **Actions**.

Detalhes: [docs/DEPLOY-GITHUB.md](docs/DEPLOY-GITHUB.md).

---

## Observabilidade

- **Health**: `/actuator/health/liveness`, `/actuator/health/readiness`
- **Métricas** (Prometheus): `saas_tenants_created_total`, `saas_policies_updated_total`, `saas_flags_toggled_total`, `saas_access_denied_total`
- **Logging**: JSON em produção, MDC com `correlationId` e `tenantId`
- **Tracing**: OpenTelemetry OTLP (configurável via env)

---

## Arquitetura do código

```
src/main/java/com/union/solutions/saascore/
├── domain/              # Entidades e regras de domínio
├── application/         # Use cases, services, ABAC evaluator
├── adapters/
│   ├── in/rest/         # Controllers REST
│   ├── in/auth/         # Filtros JWT, dev token
│   └── out/persistence/ # JPA entities, repositories
├── config/              # Security, OpenAPI, Web, JWT
├── infrastructure/      # Token issuer
└── observability/       # Correlation filter, métricas
```

Diagramas C4 e ERD em `docs/architecture/`.

---

## Testes

```bash
# Apenas unitários (não exige Docker)
./mvnw test -Dtest='!*Integration*'

# Com integração (Testcontainers + PostgreSQL)
./mvnw test -Dtest="com.union.solutions.saascore.integration.**"
```

- **Unit**: motor ABAC (default-deny, DENY, filtros plano/região), domínio Tenant.
- **Integration**: Testcontainers, CRUD completo, ABAC deny + audit; requer Docker.

---

## Troubleshooting

| Problema | Solução |
|----------|---------|
| 401 Unauthorized | Gerar token com `POST /v1/dev/token` (profile local). |
| 403 Forbidden | Verificar claims `perms`/`plan` e políticas ABAC. |
| 403 tenant mismatch | `X-Tenant-Id` deve coincidir com claim `tid`. |
| App não inicia (Docker) | `docker compose logs app`. |
| Migrations falham | Verificar conectividade com PostgreSQL e credenciais. |
| Liquibase checksum mismatch | Changesets críticos têm `validCheckSum` com versões antigas e atuais; em último caso `docker compose down -v` (apaga dados locais). |

---

## Demo Script (3–5 minutos)

1. **Subir stack** (1 min): `./scripts/up.sh`
2. **Health e docs** (30 s): http://localhost:8080/actuator/health e http://localhost:8080/docs
3. **Token e CRUD** (1 min): `./scripts/seed.sh` e mostrar tenants/flags no terminal
4. **Smoke** (1 min): `./scripts/smoke.sh`
5. **Convite de utilizador (E2E)** (30 s): `./scripts/e2e-invite-user.sh` — cria utilizador via `POST /v1/users/invite` (email único por execução)
6. **Observabilidade** (1 min): Grafana http://localhost:3030

---

## Contratos de integração

- [JWT Claims e identidade](docs/contracts/identity.md)
- [Headers HTTP](docs/contracts/headers.md)
- [Eventos Outbox](docs/contracts/events.md)
- [Compliance e auditoria](docs/compliance.md) — retenção, exportação de audit log
- [Backlog de evolução](docs/BACKLOG-EVOLUCAO.md) — checklist entregável/vendável

---

## Licença

MIT — ver [LICENSE](LICENSE).

**Autor:** Felipe Ricarte
