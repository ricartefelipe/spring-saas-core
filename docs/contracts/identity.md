# Contrato de Identidade (JWT Claims)

Este documento descreve os claims JWT esperados pelo spring-saas-core para integração com serviços Node.js, Python e frontends.

## Claims Obrigatórias

| Claim | Tipo   | Descrição                                                    |
|-------|--------|--------------------------------------------------------------|
| `sub` | string | Identificador do sujeito (ex: email ou user_id)               |
| `tid` | string | UUID do tenant ao qual o usuário pertence                     |
| `roles` | array  | Lista de roles (ex: `["admin", "user"]`)                     |
| `perms` | array  | Lista de códigos de permissão (ex: `["tenants:read", "tenants:write"]`) |
| `iss` | string | Issuer do token (validado em produção via OIDC)              |
| `iat` | number | Timestamp de emissão (Unix)                                  |
| `exp` | number | Timestamp de expiração (Unix)                                 |
| `jti` | string | ID único do token (opcional, recomendado para idempotência)  |

## Claims Opcionais (filtros ABAC)

| Claim  | Tipo   | Descrição                                                      |
|--------|--------|----------------------------------------------------------------|
| `plan` | string | Plano do tenant (ex: `free`, `pro`, `enterprise`)              |
| `region` | string | Região do tenant (ex: `us-east-1`, `eu-west-1`)              |

## Exemplo de Payload

```json
{
  "sub": "user@example.com",
  "tid": "550e8400-e29b-41d4-a716-446655440000",
  "roles": ["admin"],
  "perms": ["tenants:read", "tenants:write", "policies:read", "flags:read"],
  "plan": "enterprise",
  "region": "us-east-1",
  "iss": "https://auth.example.com/realms/saas",
  "iat": 1708704000,
  "exp": 1708707600,
  "jti": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

## Compatibilidade com IdPs

### Keycloak

- Use mappers de claim para adicionar `tid`, `perms`, `plan`, `region` no token.
- `roles` pode vir de `realm_access.roles` ou de um mapper que copie para o claim top-level `roles`.

### Auth0 / Okta / Cognito

- Configure claims customizados ou actions para incluir `tid`, `perms`, `plan`, `region`.
- Mapeie o claim de roles do IdP para `roles` no token.

## Modo Local (HS256)

No profile `local`, o endpoint `/v1/dev/token` emite tokens com qualquer payload informado. Use apenas para desenvolvimento.

## Modo Produção (OIDC)

Em produção, o token é validado pelo issuer OIDC (JWKS). Os claims devem estar presentes conforme este contrato.
