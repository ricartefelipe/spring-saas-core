# Changelog - Preparação para Venda

Mudanças aplicadas para deixar o spring-saas-core pronto para venda como núcleo de governança multi-tenant.

## P0 — Repo/Qualidade

| Path | Propósito |
|------|-----------|
| `README.md` | Badges/links apontando para ricartefelipe; Demo Script 3-5 min; links para contratos; variáveis de ambiente com nota sobre HS256 apenas em local |
| `pom.xml` | Spotless com formato YAML; dependência oauth2-resource-server para OIDC |

## P1 — Produto

### Auth Modes

| Path | Propósito |
|------|-----------|
| `src/main/resources/application-local.yml` | Já existia: token-endpoint-enabled=true, auth.mode=hs256 |
| `src/main/resources/application-prod.yml` | auth.mode fixo em oidc (sem override); dev token desabilitado |
| `src/main/java/.../config/OidcConfig.java` | **NOVO** - JwtDecoder OIDC; falha em startup se issuer/jwk-set não configurado |
| `src/main/java/.../adapters/in/auth/TokenClaims.java` | **NOVO** - DTO de claims |
| `src/main/java/.../adapters/in/auth/TokenParser.java` | **NOVO** - Interface para parsers |
| `src/main/java/.../adapters/in/auth/Hs256TokenParser.java` | **NOVO** - Parser HS256 (substitui JwtParser) |
| `src/main/java/.../adapters/in/auth/OidcTokenParser.java` | **NOVO** - Parser OIDC via JwtDecoder |
| `src/main/java/.../adapters/in/auth/JwtAuthenticationFilter.java` | Usa TokenParser em vez de JwtParser |
| `src/main/java/.../adapters/in/auth/JwtParser.java` | **REMOVIDO** - lógica migrada para Hs256TokenParser |

### Contratos

| Path | Propósito |
|------|-----------|
| `docs/contracts/identity.md` | **NOVO** - Claims JWT, compatibilidade Keycloak/Auth0 |
| `docs/contracts/headers.md` | **NOVO** - Authorization, X-Tenant-Id, X-Correlation-Id, Idempotency-Key |
| `docs/contracts/events.md` | **NOVO** - Envelope outbox, convenções, retentativas, métricas |

### Outbox

| Path | Propósito |
|------|-----------|
| `src/main/java/.../infrastructure/outbox/OutboxPublisher.java` | **NOVO** - Job agendado publica PENDING para RabbitMQ; lock TTL, retries, métricas, logs estruturados |
| `src/main/java/.../infrastructure/outbox/OutboxRabbitConfig.java` | **NOVO** - Exchange e fila para eventos |
| `src/main/java/.../adapters/out/persistence/OutboxEventJpaRepository.java` | Método findPendingReadyForDispatch |
| `src/main/java/.../observability/MetricsConfig.java` | Métricas outbox (saas_outbox_published_total, saas_outbox_failed_total) |
| `src/main/resources/application.yml` | app.outbox.exchange, routing-key-prefix |

### Observabilidade

| Path | Propósito |
|------|-----------|
| `observability/grafana/dashboards/overview.json` | Expandido; painéis saas_tenants_created_total, saas_access_denied_total |
| `observability/grafana/dashboards/outbox.json` | Métricas reais (saas_outbox_published_total, saas_outbox_failed_total) |
| `observability/grafana/dashboards/feature-flags.json` | saas_flags_toggled_total |

## P2 — Testes

| Path | Propósito |
|------|-----------|
| `src/main/java/.../application/abac/AbacEvaluator.java` | Default-deny: policies.isEmpty() → deny |
| `src/test/java/.../unit/application/abac/AbacEvaluatorTest.java` | Teste evaluate_noPolicies_returnsDeny_defaultDeny |
| `src/main/resources/db/changelog/changes/002-phase1-seed.yaml` | Seed policies: policies:write, flags:write, admin:read |
| `src/test/resources/application-test.yml` | liquibase enabled; hibernate ddl-auto: validate |
| `src/test/java/.../integration/Phase1IntegrationTest.java` | Removido override ddl-auto; Spotless:apply |

## P3 — One Command Demo

| Path | Propósito |
|------|-----------|
| `scripts/smoke.sh` | cd para repo root; já idempotente (limpa recursos criados) |

## Comandos que devem passar

```bash
./mvnw -q test          # unit tests (integration requer Docker)
./mvnw -q spotless:check
./scripts/up.sh && ./scripts/smoke.sh   # requer Docker Compose
```
