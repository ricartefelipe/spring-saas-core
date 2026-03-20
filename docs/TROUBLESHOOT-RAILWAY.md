# Railway — Healthcheck Failed / Service Offline

Se o deploy falha com "Healthcheck failed" ou "1/1 replicas never became healthy":

## 1. Ver os Deploy Logs (obrigatório)

Os **Build Logs** só mostram a construção da imagem. O erro real está nos **Deploy Logs**:

1. Railway → spring-saas-core → **Deployments**
2. Clique no deploy que falhou
3. Aba **Deploy Logs** (não Build Logs)
4. Procure por `ERROR`, `Exception`, `Connection refused`, `Failed`

## 2. Variáveis obrigatórias (staging)

| Variável | Onde obter |
|----------|------------|
| `SPRING_PROFILES_ACTIVE` | `staging` |
| `DB_URL` ou `DATABASE_URL` | Plugin PostgreSQL → `${{Postgres.DATABASE_URL}}` ou `${{Postgres.JDBC_DATABASE_URL}}` |
| `DB_USER` | `${{Postgres.PGUSER}}` |
| `DB_PASS` | `${{Postgres.PGPASSWORD}}` |
| `REDIS_HOST` | Plugin Redis → `${{Redis.REDISHOST}}` |
| `REDIS_PORT` | `${{Redis.REDISPORT}}` |
| `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASS` | CloudAMQP ou plugin |
| `JWT_HS256_SECRET` | `openssl rand -base64 32` |

## 3. Erros comuns

| Erro nos Deploy Logs | Causa | Solução |
|----------------------|-------|---------|
| `Connection to localhost:5435 refused` | DB_URL não configurado | Adicionar variáveis do PostgreSQL |
| `Redis connection failed` | REDIS_HOST não configurado | Adicionar variáveis do Redis |
| `RabbitMQ` / `AMQP` | RABBITMQ_* não configurado | Configurar CloudAMQP ou plugin |
| `IllegalStateException` / `app.email` | Resend sem domínio | Ver CONVITE-EMAIL-DEPLOY.md |

## 4. Rede do Railway

- PostgreSQL e Redis devem estar no **mesmo projeto** Railway (para rede interna)
- O serviço spring-saas-core precisa referenciar `${{Postgres.DATABASE_URL}}` etc.
- Se usar CloudAMQP fora do Railway, a URL deve ser acessível pública
