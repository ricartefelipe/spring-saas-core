# Privacy Policy — spring-saas-core

Data privacy policy for the spring-saas-core control plane and the Fluxe B2B platform.

Last updated: 2026-03-07

---

## 1. Data collected

spring-saas-core processes the minimum data required to operate the multi-tenant control plane:

| Category | Data | Purpose |
|----------|------|---------|
| Tenant metadata | Name, plan, region, status | Multi-tenant governance and routing |
| User identifiers | JWT `sub` claim (OIDC subject) | Authentication, ABAC evaluation, audit trail |
| Roles and permissions | JWT `roles` and `perms` claims | Authorization (RBAC/ABAC) |
| Audit logs | Actor, action, resource, timestamp, status code, correlation ID | Security auditing, compliance, anomaly detection |
| Feature flags | Flag name, enabled state, rollout config, allowed roles | Per-tenant feature management |
| ABAC policies | Permission code, effect, allowed plans/regions | Access control governance |

---

## 2. Data NOT collected

- **No personal data beyond JWT claims.** The service does not store names, emails, phone numbers or addresses of end users beyond what the identity provider places in the JWT `sub` claim.
- **No raw payment or card data.** Payment processing is handled exclusively by py-payments-ledger; spring-saas-core never receives or stores card numbers, bank accounts or payment credentials.
- **No browser fingerprints or tracking cookies.** The service is an API-only backend with no client-side tracking.
- **No IP address logging by default.** Client IPs are not persisted in audit logs unless explicitly configured at the infrastructure level.

---

## 3. Data retention

| Data type | Retention | Configuration |
|-----------|-----------|---------------|
| Audit logs | Configurable | `AUDIT_RETENTION_DAYS` environment variable (default: 90 days) |
| Tenant metadata | Retained while tenant is active | Removed on tenant deletion |
| Policies & flags | Soft-deleted (retained for audit) | Purged according to retention policy |

Records beyond the configured retention period may be archived or permanently removed according to the operator's internal policy. See [`docs/compliance.md`](compliance.md) for details.

---

## 4. Data export

Authorized users can export audit data through the API:

- **Endpoint:** `GET /v1/audit/export`
- **Required parameters:** `from` and `to` (ISO 8601 date range)
- **Formats:** `format=json` or `format=csv`
- **Limit:** Up to 10,000 records per export request
- **Access control:** Protected by ABAC — only users with the appropriate permission can export

This supports data portability requirements under LGPD (Art. 18, V) and GDPR (Art. 20).

---

## 5. Data deletion

- **Tenant deletion** triggers removal of all associated data: tenant metadata, policies scoped to the tenant, feature flags, and audit log entries (after the retention period).
- **Soft-delete** is used for policies and flags to preserve audit integrity within the retention window.
- **Right to erasure** requests should be directed to the platform operator, who can invoke tenant deletion via the API (`DELETE /v1/tenants/{id}`).
- After deletion, identifiers in historical audit entries are anonymized or removed according to the configured retention policy.

---

## 6. Third-party data sharing

**spring-saas-core does not share tenant data with third parties.**

- All data remains within the tenant's scope and is not transferred to external services.
- Cross-service communication (with node-b2b-orders and py-payments-ledger) occurs through internal event bus (RabbitMQ) within the platform boundary, using signed JWTs and tenant-scoped snapshots.
- No analytics, advertising or data broker integrations exist.

---

## 7. Data residency

- Each tenant has a configured `region` field that defines where its data should be processed and stored.
- The platform respects this configuration when routing requests and replicating data.
- Operators deploying across multiple regions are responsible for ensuring infrastructure-level data residency (e.g., database placement, message broker topology).
- Tenant region is included in JWT claims and event payloads, allowing downstream services to enforce residency constraints.

---

## 8. Compliance references

spring-saas-core follows privacy principles aligned with:

| Framework | Relevant principles |
|-----------|-------------------|
| **LGPD** (Lei Geral de Proteção de Dados — Brazil) | Purpose limitation, data minimization, transparency, security, data portability (Art. 18) |
| **GDPR** (General Data Protection Regulation — EU) | Lawfulness, purpose limitation, data minimization, storage limitation, integrity and confidentiality (Art. 5) |

Key compliance capabilities:

- **Audit trail** of all sensitive actions and access denials
- **Configurable retention** with export capabilities
- **Default-deny** access control (ABAC with DENY precedence)
- **No unnecessary PII** — only JWT subject identifiers are stored
- **Tenant isolation** — data is scoped and never shared across tenants
- **Data export** via API for portability and audit requests

For full audit and compliance documentation, see [`docs/compliance.md`](compliance.md).

---

## 9. Contact for data requests

For data access, rectification, deletion or portability requests, contact the platform operator:

- **Email:** privacy@fluxe.io
- **Subject line:** `[Data Request] — <tenant name or ID>`
- **Expected response time:** 15 business days (aligned with LGPD Art. 18, §5)

The operator is the data controller. spring-saas-core acts as the data processor providing the technical infrastructure for multi-tenant governance.
