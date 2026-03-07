# Prompt de Evolução — spring-saas-core

Este documento define o **prompt de evolução** do projeto. Use-o como contexto em decisões de arquitetura, backlog e evolução contínua.

---

## Identidade

- **spring-saas-core** = control plane multi-tenant da plataforma B2B.
- Centraliza: tenants (planos, regiões, status), governança ABAC/RBAC, feature flags, auditoria, JWT padronizado, outbox (RabbitMQ).
- Única fonte de verdade para tenants, políticas e flags; **node-b2b-orders** e **py-payments-ledger** validam o JWT e aplicam as mesmas regras.

---

## Objetivo: entregável e vendável

Priorizar evolução que aproxime o projeto destes critérios:

| Área | Critério de "pronto para venda" |
|------|----------------------------------|
| **Funcional** | CRUD tenants/políticas/flags; auditoria consultável; JWT e snapshots para orders/payments; outbox confiável. |
| **Segurança** | ABAC com DENY precedente, default-deny, OIDC em produção; auditoria de ACCESS_DENIED; sem credenciais em código. |
| **Operacional** | Health, Prometheus, OpenAPI, scripts up/migrate/seed/smoke, deploy reproduzível. |
| **Contratos** | Documentação de identidade/headers/eventos alinhada com orders e payments; API v1 estável. |
| **Compliance** | Auditoria de ações sensíveis e negações; retenção/exportação de audit log. |

Preservar sempre: multi-tenancy, ABAC, integração com Fluxe B2B Suite + orders + payments.

---

## Critérios detalhados

### Funcional

- CRUD completo de tenants, políticas ABAC e feature flags
- Auditoria consultável via `GET /v1/audit` com filtros
- JWT padronizado e snapshots de tenant (`GET /v1/tenants/{id}/snapshot`) para orders e payments
- Outbox confiável para eventos de domínio (JpaOutboxPublisher + RabbitMQ)

### Segurança

- ABAC com regra DENY precedente e default-deny
- OIDC configurado para produção (RS256, issuer validation)
- Auditoria de todas as tentativas ACCESS_DENIED
- Sem credenciais hardcoded em código

### Operacional

- Health checks: `/healthz`, `/readyz`
- Métricas Prometheus
- OpenAPI (YAML + JSON)
- Scripts: up, migrate, seed, smoke
- Deploy reproduzível (Docker, docker-compose)

### Contratos

- Documentação de identidade JWT (`docs/contracts/identity.md`)
- Documentação de headers (`docs/contracts/headers.md`)
- Documentação de eventos (`docs/contracts/events.md`)
- API v1 estável

### Compliance

- Auditoria de ações sensíveis (CRUD)
- Auditoria de negações (ACCESS_DENIED)
- Retenção e exportação de audit log (ver `docs/compliance.md`)

---

## IA/LLM no serviço

O projeto deve habilitar uso de IA/LLM para:

- Recomendações de governança
- Análise de auditoria e detecção de anomalias
- Onboarding de tenants
- Documentação viva
- Feedback em ciclo fechado

### Requisitos para IA/LLM

- Dados para aprendizado (seguros/agregados)
- APIs estáveis para agentes: `/v1/tenants`, `/v1/audit`, `/v1/tenants/{id}/snapshot`
- Eventos documentados (`docs/contracts/events.md`)
- Segurança: ABAC, sem PII desnecessário, auditoria de acesso

---

## Uso no Cursor

- **Desenvolvimento:** ao sugerir ou implementar mudanças, priorizar itens que aproximem dos critérios acima e habilitem IA/LLM.
- **Revisão de arquitetura:** garantir que novas features não quebrem contratos com node-b2b-orders e py-payments-ledger.
- **Detalhes:** critérios finos, papel da IA e diretrizes técnicas estão neste documento e em `docs/BACKLOG-EVOLUCAO.md`.
- **Compliance:** auditoria, retenção e exportação em `docs/compliance.md`.

---

## Resumo em uma frase

O spring-saas-core é o control plane multi-tenant da plataforma B2B, pronto para venda quando cumprir os critérios funcionais, de segurança, operacional, contratos e compliance acima, com suporte à integração com agentes IA/LLM.
