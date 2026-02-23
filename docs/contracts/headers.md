# Contratos de Headers HTTP

Este documento descreve os headers esperados e emitidos pela API do spring-saas-core.

## Headers de Request

### Authorization (obrigatório)

- **Valor**: `Bearer <JWT>`
- **Descrição**: Token JWT válido. Em modo local, use o endpoint `/v1/dev/token` para gerar.

### X-Tenant-Id (obrigatório em endpoints tenant-scoped)

- **Valor**: UUID do tenant (ex: `550e8400-e29b-41d4-a716-446655440000`)
- **Descrição**: Deve coincidir com a claim `tid` do JWT. Endpoints como `/v1/tenants/{id}/flags` exigem esse header.
- **Validação**: Se enviado, deve ser igual a `tid`. Se `tid` estiver no token e o header divergir, retorna 403.

### X-Correlation-Id (opcional)

- **Valor**: String única por requisição (ex: UUID ou slug)
- **Descrição**: Usado para rastreamento distribuído. Se ausente, gerado automaticamente.
- **Resposta**: O valor usado (gerado ou propagado) é retornado no header `X-Correlation-Id` da resposta.

### Idempotency-Key (quando aplicável)

- **Valor**: String única por operação (ex: UUID)
- **Descrição**: Para operações idempotentes (ex: criação de recursos), use esse header para evitar duplicatas. O valor deve ser único por requisição lógica.
- **Endpoints**: POST `/v1/tenants`, POST `/v1/policies`, POST `/v1/tenants/{id}/flags`.

## Headers de Response

### X-Correlation-Id

- Sempre presente nas respostas; propaga o valor da request ou retorna o gerado.

## Exemplo (cURL)

```bash
curl -X POST "http://localhost:8080/v1/tenants" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Correlation-Id: req-abc-123" \
  -H "Idempotency-Key: create-tenant-xyz-001" \
  -d '{"name":"Acme Corp","plan":"pro","region":"us-east-1"}'
```

## Integração Node.js / Python

- Propagar `X-Correlation-Id` em chamadas internas entre serviços.
- Incluir `X-Tenant-Id` em todas as chamadas ao spring-saas-core quando o contexto for tenant-scoped.
- Armazenar o `Authorization` header do request original ao fazer chamadas em nome do usuário.
