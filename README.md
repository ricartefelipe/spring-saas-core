# spring-saas-core

**Golden Path: menos hype, mais entrega** — Núcleo de governança multi-tenant para SaaS: tenants, usuários, RBAC/ABAC, feature flags, auditoria e trilha confiável (outbox).

[![Build](https://github.com/yourorg/spring-saas-core/actions/workflows/ci.yml/badge.svg)](https://github.com/yourorg/spring-saas-core/actions/workflows/ci.yml)
[![Coverage](https://img.shields.io/badge/coverage-jacoco-green)](https://github.com/yourorg/spring-saas-core)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Docker](https://img.shields.io/badge/docker-ready-brightgreen)](docker-compose.yml)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-yellow)](docs/api/openapi.yaml)
[![Prometheus](https://img.shields.io/badge/Prometheus-metrics-orange)](http://localhost:9090)

---

## Por que isso importa

Engenharia **operável**: causalidade (correlation_id, trace_id), decisões auditadas (ABAC/RBAC), replicação eventual documentada, rate limit e circuit breakers expostos. Menos “framework hype” e mais fundamentos embutidos no caminho padrão.

## Highlights

- **Multi-tenant** com isolamento por tenant_id e sharding (tenant → shard → datasource)
- **RBAC + ABAC**: roles/permissions + políticas por plan, region, ip_allowlist/time_window
- **Outbox** + RabbitMQ + DLQ: estado e evento na mesma transação; dispatcher com lock distribuído
- **Multi-região (demo)**: region-a / region-b, 2 Postgres, roteamento por primaryRegion + Consistency (strong/eventual)
- **Sharding**: mapping tenant → shard; DAL resolve shard antes da transação
- **Feature flags**: on/off, rollout %, targeting (plan/region/role), cache Redis + invalidação
- **Rate limit** distribuído (Redis, token bucket/sliding window), headers X-RateLimit-*
- **Chaos engineering** controlado: latência, % erro, drop de mensagens por tenant (admin)
- **Observabilidade**: logs JSON, Micrometer/Prometheus, OpenTelemetry OTLP, Grafana provisionado

## Demo em 10 minutos

```bash
# Subir toda a stack
./scripts/up.sh

# Seeds (tenants, users, roles, policies, flags)
./scripts/seed.sh

# Smoke end-to-end + cenário 403
./scripts/smoke.sh

# Chaos controlado + recuperação
./scripts/chaos-demo.sh
```

Ou direto:

```bash
docker compose up -d --build
# Aguardar readiness (~30s), depois:
./scripts/seed.sh && ./scripts/smoke.sh
```

**Usuários seed:** `admin@example.com` e `userb@example.com` — senha: **password**

**URLs após `up.sh`:**

| Serviço        | URL |
|----------------|-----|
| App (region-a) | http://localhost:8080 |
| App (region-b) | http://localhost:8081 |
| Swagger (A)    | http://localhost:8080/docs |
| Swagger (B)    | http://localhost:8081/docs |
| Grafana        | http://localhost:3000 (admin/admin) |
| Prometheus     | http://localhost:9090 |
| RabbitMQ UI    | http://localhost:15672 (guest/guest) |

## Golden Path: como criar um endpoint novo

1. **Domain**: entidade/VO em `domain/`; invariantes e regras no agregado.
2. **Application**: use case em `application/` (transação, orquestração); portas in/out.
3. **Adapters in**: controller em `adapters/in/`, DTOs e mapeadores; sem regra de negócio.
4. **Adapters out**: implementação JPA/Redis/Rabbit em `adapters/out/` se necessário.
5. **Validação**: Bean Validation nos DTOs; validação de negócio no use case.
6. **Auth**: `@PreAuthorize` ou PermissionEvaluator; ABAC no service de autorização.
7. **Logging/correlation**: headers X-Tenant-Id, X-Correlation-Id, X-Region; MDC e logs JSON.
8. **Métricas**: Micrometer em pontos críticos; circuit breaker/timeout onde fizer sentido.
9. **Testes**: unit (domain/application) + integration (controller + segurança + idempotência).

## Troubleshooting rápido

| Problema | Ação |
|----------|------|
| 401 ao chamar API | Obter token em `POST /v1/auth/token`; enviar `Authorization: Bearer <token>`. |
| 403 tenant mismatch | `X-Tenant-Id` deve coincidir com claim `tid` do token (ou `tid=*` para admin). |
| Replicação eventual “atrasada” | Write no primary; leitura eventual na outra região pode levar alguns segundos. |
| Rate limit 429 | Ver headers X-RateLimit-Limit, X-RateLimit-Remaining, Retry-After. |
| Outbox não despacha | Verificar RabbitMQ e job de dispatcher; métricas outbox_dispatch_* no Prometheus. |

## Stack

- Java 21, Spring Boot 3.x (Maven), Spring Security (JWT Resource Server), Spring Data JPA, Liquibase (YAML)
- PostgreSQL (multi-region demo: 2 instâncias), Redis, RabbitMQ
- Micrometer + Prometheus, OpenTelemetry OTLP, Grafana
- Resilience4j, springdoc-openapi

## Licença

MIT — ver [LICENSE](LICENSE).
