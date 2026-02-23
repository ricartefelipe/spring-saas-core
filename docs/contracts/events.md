# Contratos de Eventos (Outbox)

O spring-saas-core persiste eventos de domínio na tabela `outbox_events` na mesma transação das mutações. Um publisher opcional envia os eventos para RabbitMQ.

## Envelope de Evento

### Estrutura

| Campo            | Tipo   | Descrição                                              |
|-----------------|--------|--------------------------------------------------------|
| `id`            | uuid   | ID único do evento                                     |
| `aggregateType` | string | Tipo do agregado (ex: `TENANT`, `POLICY`, `FLAG`)       |
| `aggregateId`   | string | ID do agregado afetado                                  |
| `eventType`     | string | Nome do evento (ex: `tenant.created`, `tenant.updated`) |
| `payload`       | json   | Dados do evento (objeto)                                |
| `createdAt`     | string | ISO-8601 timestamp                                     |

## Convenção de Nomenclatura

- **aggregateType**: UPPERCASE, singular (TENANT, POLICY, FLAG)
- **eventType**: `{aggregate}.{action}` em lowercase (tenant.created, policy.deleted)

## Eventos Conhecidos

| aggregateType | eventType        | Payload típico                                      |
|---------------|------------------|-----------------------------------------------------|
| TENANT        | tenant.created   | `{name, plan, region}`                              |
| TENANT        | tenant.updated   | `{name?, plan?, region?, status?}`                   |
| TENANT        | tenant.deleted   | `{reason?}`                                         |
| POLICY        | policy.created   | `{permissionCode, effect}`                           |
| POLICY        | policy.updated   | `{permissionCode?, effect?, enabled?}`               |
| FLAG          | flag.created     | `{name, enabled, rolloutPercent}`                   |
| FLAG          | flag.toggled     | `{name, enabled}`                                    |

## Publicação para RabbitMQ

Quando `OUTBOX_PUBLISH_ENABLED=true`, eventos PENDING são publicados para a exchange configurada.

### Configuração

- **Routing key**: `{aggregateType}.{eventType}` (ex: `TENANT.tenant.created`)
- **Exchange**: Configurável via propriedades (default: fanout ou topic conforme setup)

### Retentativas

- `retry-max`: número máximo de tentativas (default: 5)
- `lock-ttl-seconds`: TTL do lock para evitar processamento duplicado (default: 60)
- Backoff entre retentativas: exponencial

### Métricas

- `saas_outbox_published_total`: eventos publicados com sucesso
- `saas_outbox_failed_total`: eventos que atingiram retry-max

### Logs Estruturados

- Publicação: `event_id`, `aggregate_type`, `event_type`, `attempt`
- Falha: `event_id`, `error`, `attempt`, `max_retries`
