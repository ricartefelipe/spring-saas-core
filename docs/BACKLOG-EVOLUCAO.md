# Backlog de Evolução

Estado atual por critério de "pronto para venda".

---

## Funcional

- [x] CRUD tenants com paginação cursor-based
- [x] CRUD políticas ABAC
- [x] CRUD feature flags por tenant
- [x] Auditoria consultável via GET /v1/audit
- [x] JWT padronizado (claims: sub, tid, roles, perms, plan, region)
- [x] Tenant snapshots (GET /v1/tenants/{id}/snapshot)
- [x] Outbox para eventos de domínio (JpaOutboxPublisher + OutboxPublisher)
- [ ] Webhook de eventos para integradores externos

---

## Segurança

- [x] ABAC com DENY precedente e default-deny
- [x] Auditoria de ACCESS_DENIED
- [x] OIDC configurado para produção
- [x] ABAC em TenantSnapshotController
- [x] Sem credenciais hardcoded em código
- [ ] Rate limiting por tenant
- [ ] Rotação de JWT_SECRET sem downtime

---

## Operacional

- [x] Health checks (/healthz, /readyz)
- [x] Métricas Prometheus
- [x] OpenAPI (YAML + JSON)
- [x] Docker multi-stage
- [x] Scripts: up, migrate, seed
- [ ] Script de smoke test
- [ ] Alertas Grafana pré-configurados

---

## Contratos

- [x] docs/contracts/events.md
- [x] docs/contracts/identity.md
- [x] docs/contracts/headers.md
- [x] API v1 estável
- [ ] Versionamento de contratos

---

## Compliance

- [x] Auditoria de ações sensíveis
- [x] Auditoria de negações
- [ ] Retenção configurável de audit log
- [ ] Exportação de audit log (CSV/JSON)
- [ ] Política de privacidade de dados

---

## IA/LLM

- [ ] API de dados agregados para análise
- [ ] Endpoint de anomalias em audit log
- [ ] Documentação viva gerada por IA
