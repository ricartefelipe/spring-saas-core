# spring-saas-core

Núcleo de governança multi-tenant para SaaS: tenants, RBAC/ABAC, feature flags, auditoria e publicação de eventos (outbox).

[![Build](https://github.com/ricartefelipe/spring-saas-core/actions/workflows/ci.yml/badge.svg)](https://github.com/ricartefelipe/spring-saas-core/actions/workflows/ci.yml)
[![Coverage](https://img.shields.io/badge/coverage-jacoco-green)](https://github.com/ricartefelipe/spring-saas-core)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Docker](https://img.shields.io/badge/docker-ready-brightgreen)](docker-compose.yml)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-yellow)](docs/api/openapi.yaml)
[![Prometheus](https://img.shields.io/badge/Prometheus-metrics-orange)](http://localhost:9090)

---

## Visão Geral

O `spring-saas-core` é o control plane (admin) de uma plataforma SaaS, responsável por:

- **Gestão de Tenants** (clientes): CRUD completo com plano, região e status
- **Políticas ABAC/RBAC**: autorização por permission_code com filtros por plano/região, DENY com precedência, default-deny
- **Feature Flags por tenant**: on/off, rollout percentual, targeting por roles
- **Auditoria completa**: todas as ações (CRUD) e negações (ACCESS_DENIED) registradas
- **Contrato de identidade**: claims JWT padronizadas (`tid`, `roles`, `perms`, `plan`, `region`, `sub`) compatíveis com serviços Node.js e Python
- **Outbox pattern**: eventos de domínio persistidos na mesma transação, prontos para publicação via RabbitMQ

## Stack

- Java 21, Spring Boot 3.x (Maven)
- Spring Security (JWT Resource Server), Spring Data JPA
- PostgreSQL, Liquibase (YAML)
- Redis (opcional), RabbitMQ (opcional)
- Micrometer + Prometheus, OpenTelemetry OTLP
- Grafana, springdoc-openapi

## Quick Start

### Docker Compose (recomendado)

```bash
./scripts/up.sh
./scripts/seed.sh
./scripts/smoke.sh
```

### Local (Maven)

```bash
docker compose up -d postgres redis rabbitmq
./mvnw spring-boot:run
```

### URLs

| Serviço     | URL                                    |
|-------------|----------------------------------------|
| App         | http://localhost:8080                   |
| Swagger UI  | http://localhost:8080/docs              |
| OpenAPI     | http://localhost:8080/v3/api-docs       |
| Health      | http://localhost:8080/actuator/health   |
| Prometheus  | http://localhost:8080/actuator/prometheus |
| Grafana     | http://localhost:3030 (admin/admin)     |
| RabbitMQ UI | http://localhost:15672 (guest/guest)    |

## API REST (prefixo /v1)

### Tenants

| Método | Endpoint             | Descrição                         |
|--------|----------------------|-----------------------------------|
| POST   | `/v1/tenants`        | Criar tenant                      |
| GET    | `/v1/tenants`        | Listar (paginado, filtros)        |
| GET    | `/v1/tenants/{id}`   | Obter por ID                      |
| PATCH  | `/v1/tenants/{id}`   | Atualizar                         |
| DELETE | `/v1/tenants/{id}`   | Soft delete (SUSPENDED)           |

### Policies (ABAC)

| Método | Endpoint              | Descrição                         |
|--------|-----------------------|-----------------------------------|
| POST   | `/v1/policies`        | Criar política                    |
| GET    | `/v1/policies`        | Listar (paginado, filtros)        |
| GET    | `/v1/policies/{id}`   | Obter por ID                      |
| PATCH  | `/v1/policies/{id}`   | Atualizar                         |
| DELETE | `/v1/policies/{id}`   | Remover                           |

### Feature Flags (por tenant)

| Método | Endpoint                              | Descrição            |
|--------|---------------------------------------|----------------------|
| POST   | `/v1/tenants/{tenantId}/flags`        | Criar flag           |
| GET    | `/v1/tenants/{tenantId}/flags`        | Listar flags         |
| PATCH  | `/v1/tenants/{tenantId}/flags/{name}` | Atualizar flag       |
| DELETE | `/v1/tenants/{tenantId}/flags/{name}` | Remover flag         |

### Audit Log

| Método | Endpoint     | Descrição                                                  |
|--------|--------------|------------------------------------------------------------|
| GET    | `/v1/audit`  | Consultar (paginado; filtros: tenantId, action, from/to)   |

### Consumer Endpoints (para serviços Node/Python)

| Método | Endpoint                          | Descrição                     |
|--------|-----------------------------------|-------------------------------|
| GET    | `/v1/tenants/{id}/snapshot`       | Snapshot do tenant            |
| GET    | `/v1/tenants/{id}/policies`       | Políticas aplicáveis          |
| GET    | `/v1/tenants/{id}/flags`          | Flags do tenant               |

### Dev Token (somente profile local)

| Método | Endpoint          | Descrição                                |
|--------|-------------------|------------------------------------------|
| POST   | `/v1/dev/token`   | Gerar JWT HS256 para testes locais       |

### Outros

| Método | Endpoint    | Descrição              |
|--------|-------------|------------------------|
| GET    | `/v1/me`    | Info do usuário atual  |
| GET    | `/healthz`  | Liveness               |
| GET    | `/readyz`   | Readiness              |

## Autorização (ABAC)

1. JWT validado (HS256 local / OIDC em produção)
2. `X-Tenant-Id` deve bater com claim `tid`
3. Permission codes exigidos por endpoint
4. Políticas avaliadas: `enabled=true AND (plan in allowed_plans OR vazio) AND (region in allowed_regions OR vazio)`
5. **DENY tem precedência** sobre ALLOW
6. **Default-deny**: sem política aplicável = acesso negado
7. Negações registradas no `audit_log` com action `ACCESS_DENIED`

## Claims JWT

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

## Headers

| Header            | Obrigatório | Descrição                                      |
|-------------------|-------------|-------------------------------------------------|
| Authorization     | Sim         | `Bearer <JWT>`                                  |
| X-Tenant-Id       | Sim*        | ID do tenant (validado contra claim `tid`)      |
| X-Correlation-Id  | Não         | Se ausente, gerado automaticamente              |

## Variáveis de Ambiente

| Variável                  | Default                          | Descrição                       |
|---------------------------|----------------------------------|---------------------------------|
| `SPRING_PROFILES_ACTIVE`  | `local`                          | Profile ativo                   |
| `DB_URL`                  | `jdbc:postgresql://...`          | URL do PostgreSQL               |
| `DB_USER`                 | `saascore`                       | Usuário do banco                |
| `DB_PASS`                 | `saascore`                       | Senha do banco                  |
| `AUTH_MODE`               | `hs256`                          | `hs256` (local) ou `oidc` (prod) |
| `JWT_ISSUER`              | `spring-saas-core`               | Issuer do JWT                   |
| `JWT_HS256_SECRET`        | (dev secret)                     | Chave HS256 (**somente profile local**) |
| `OIDC_ISSUER_URI`         | *obrigatório em prod*            | URI do issuer OIDC (ex: Keycloak) |
| `OIDC_JWK_SET_URI`        | (derivado do issuer)            | URI do JWK Set OIDC             |
| `REDIS_HOST`              | `localhost`                      | Host Redis                      |
| `RABBITMQ_HOST`           | `localhost`                      | Host RabbitMQ                   |
| `OUTBOX_PUBLISH_ENABLED`  | `false`                          | Habilitar publicação outbox     |
| `SERVER_PORT`             | `8080`                           | Porta do servidor               |

## Scripts

| Script                    | Descrição                                        |
|---------------------------|--------------------------------------------------|
| `./scripts/up.sh`         | Sobe toda a stack via Docker Compose             |
| `./scripts/migrate.sh`    | Verifica status das migrações                    |
| `./scripts/seed.sh`       | Verifica dados seed (aplicados via Liquibase)    |
| `./scripts/smoke.sh`      | Suite de smoke tests automatizados               |
| `./scripts/api-export.sh` | Exporta OpenAPI JSON/YAML para `docs/api/`       |

## Observabilidade

- **Health**: `/actuator/health/liveness`, `/actuator/health/readiness`
- **Métricas**: `/actuator/prometheus` (Micrometer)
  - `saas_tenants_created_total`
  - `saas_policies_updated_total`
  - `saas_flags_toggled_total`
  - `saas_access_denied_total`
- **Logging**: JSON estruturado em produção, MDC com `correlationId` e `tenantId`
- **Tracing**: OpenTelemetry OTLP (configurável via env)

## Arquitetura

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

Diagramas C4 e ERD disponíveis em `docs/architecture/`.

## Testes

```bash
./mvnw test -Dtest="com.union.solutions.saascore.unit.**"
./mvnw test -Dtest="com.union.solutions.saascore.integration.**"
```

- **Unit tests**: ABAC policy engine (default-deny, DENY precedência, filtros plano/região), domain Tenant
- **Integration tests**: Testcontainers (PostgreSQL), CRUD completo, ABAC deny + audit — requer Docker

## Troubleshooting

| Problema                    | Solução                                                         |
|-----------------------------|-----------------------------------------------------------------|
| 401 Unauthorized            | Gerar token com `POST /v1/dev/token`                            |
| 403 Forbidden               | Verificar claims `perms`/`plan` e políticas ABAC                |
| 403 tenant mismatch         | `X-Tenant-Id` deve coincidir com claim `tid`                    |
| App não inicia (Docker)     | `docker compose logs app` para ver erros                        |
| Migrations falham           | Verificar conectividade com PostgreSQL                          |

## Demo Script (3–5 minutos)

Para demonstrar o núcleo em uma reunião de vendas:

1. **Subir a stack** (1 min): `./scripts/up.sh`
2. **Health e docs** (30 s): Abrir http://localhost:8080/actuator/health e http://localhost:8080/docs
3. **Gerar token e CRUD** (1 min): Rodar `./scripts/seed.sh` e mostrar tenants/ flags no terminal
4. **Smoke completo** (1 min): `./scripts/smoke.sh` – valida health, CRUD, ABAC deny + audit
5. **Observabilidade** (1 min): Grafana http://localhost:3030 (admin/admin) – dashboards de tenants, policies, outbox

## Contratos de Integração

- [JWT Claims e identidade](docs/contracts/identity.md)
- [Headers HTTP](docs/contracts/headers.md)
- [Eventos Outbox](docs/contracts/events.md)

## Autor

**Felipe Ricarte** - felipericartem@gmail.com

## Licença

MIT - ver [LICENSE](LICENSE).
