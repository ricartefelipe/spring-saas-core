# Compliance and Audit

Documentação das capacidades de compliance e auditoria do spring-saas-core e da plataforma B2B integrada.

---

## Audit logging

Todas as operações CRUD e eventos de acesso negado são registrados no audit log.

### Eventos auditados

| Category | Events |
|----------|--------|
| CRUD | Create, Read, Update, Delete em tenants, policies, feature flags |
| Access | `ACCESS_DENIED` — todas as tentativas de acesso negadas por ABAC |
| System | `audit.retention.cleanup` — execução do job de limpeza de audit log |
| System | `JWT_VERIFIED_WITH_PREVIOUS_KEY` — token verificado com chave anterior durante rotação de JWT_SECRET |

### Campos registrados

| Field | Description |
|-------|-------------|
| `actor` | Identificador do ator (sub do JWT) |
| `roles` | Roles do ator no momento da ação |
| `permissions` | Permissões relevantes |
| `action` | Ação executada (CREATE, READ, UPDATE, DELETE, ACCESS_DENIED) |
| `resource` | Recurso afetado (tenant, policy, flag, etc.) |
| `statusCode` | Código HTTP retornado |
| `correlationId` | ID de rastreamento da requisição |
| `timestamp` | Data/hora da ocorrência (ISO 8601) |
| `tenantId` | Tenant no contexto (quando aplicável) |

---

## ABAC auditing

- Todas as negações de acesso são registradas com `action=ACCESS_DENIED`
- O motivo da negação pode incluir: política DENY aplicada, falta de permissão, tenant mismatch, default-deny
- Esses registros permitem auditoria de tentativas de acesso não autorizado para fins de segurança e compliance

---

## Retention policy

- **Configurável:** Período de retenção do audit log é configurável (sugestão: 90 dias por padrão)
- **Variável sugerida:** `AUDIT_RETENTION_DAYS` (default: 90)
- Registros além do período podem ser arquivados ou removidos conforme política interna

---

## Export

### Endpoint

`GET /v1/audit` — consulta paginada com filtros

### Filtros disponíveis

| Parameter | Description |
|-----------|-------------|
| `from` | Data início (ISO 8601) |
| `to` | Data fim (ISO 8601) |
| `action` | Tipo de ação (CREATE, READ, UPDATE, DELETE, ACCESS_DENIED) |
| `tenantId` | Filtrar por tenant |
| `actor` | Filtrar por ator (sub) |

### Export format

`GET /v1/audit/export` — exportação para compliance:

- Parâmetros obrigatórios: `from`, `to`
- Formatos: `format=json` ou `format=csv`
- Limite típico: até 10.000 registros por exportação

---

## PII considerations

- **Evitar PII no audit payload:** Não armazenar dados pessoais identificáveis desnecessários no conteúdo do log
- **Usar referências:** Preferir IDs (tenantId, resourceId) em vez de nomes, e-mails ou dados sensíveis
- **Actor:** O campo `actor` pode conter o `sub` do JWT (ex.: email ou OIDC subject); avaliar política de privacidade quanto ao que é necessário para auditoria vs. minimização de dados

---

## Data model (audit entry)

Exemplo de estrutura de um registro de auditoria:

```json
{
  "id": "uuid",
  "occurredAt": "2025-03-05T12:00:00Z",
  "actor": "user@example.com",
  "roles": ["admin"],
  "permissions": ["tenants:write"],
  "action": "UPDATE",
  "resource": "tenant",
  "resourceId": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "statusCode": 200,
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

Para `ACCESS_DENIED`:

```json
{
  "action": "ACCESS_DENIED",
  "resource": "tenant",
  "reason": "Policy DENY applied for plan free",
  "statusCode": 403
}
```
