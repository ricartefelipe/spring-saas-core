# Subir todos os projetos e testar

Guia para levantar **spring-saas-core**, **node-b2b-orders**, **py-payments-ledger** e **fluxe-b2b-suite** e rodar os testes de fumaça.

## Pré-requisitos

- **Docker** e **Docker Compose**
- **Rede compartilhada**: todos os serviços usam a rede `fluxe_shared`. Crie uma vez:
  ```bash
  docker network create fluxe_shared
  ```
- **jq** (para scripts de smoke do spring-saas-core): `sudo apt install jq` ou equivalente
- **Node 20+** e **pnpm** (para fluxe-b2b-suite, se for rodar a UI em modo dev)

---

## Ordem recomendada

1. **spring-saas-core** (control plane: tenants, JWT, políticas, auditoria)
2. **node-b2b-orders** (API de pedidos)
3. **py-payments-ledger** (API de pagamentos / ledger)
4. **fluxe-b2b-suite** (UI: ops-portal, shop, admin-console) — opcional

---

## 1. spring-saas-core

### Opção A: Docker Compose (app + infra)

Requer que o build Docker tenha acesso à internet (Maven). No `docker-compose.yml` o build do serviço `app` já está com `network: host` para evitar falhas de rede no build.

```bash
cd /home/frm/Documentos/wks/spring-saas-core
./scripts/up.sh
./scripts/seed.sh
```

URLs: App http://localhost:8080 | Swagger http://localhost:8080/docs | RabbitMQ http://localhost:15675 (guest/guest) | Grafana http://localhost:3030 (admin/admin).

### Opção B: Só infra no Docker + app no host

Se o build em Docker falhar por rede (ex.: `Unknown host repo.maven.apache.org`):

```bash
cd /home/frm/Documentos/wks/spring-saas-core
docker network create fluxe_shared
docker compose up -d postgres redis rabbitmq
# Aguardar ~10s e rodar o app no host:
SPRING_PROFILES_ACTIVE=local \
  DB_URL=jdbc:postgresql://localhost:5435/saascore \
  DB_USER=saascore DB_PASS=saascore \
  REDIS_HOST=localhost REDIS_PORT=6382 \
  RABBITMQ_HOST=localhost RABBITMQ_PORT=5675 \
  ./mvnw spring-boot:run
```

Depois, em outro terminal:

```bash
./scripts/seed.sh
```

### Testar (smoke)

```bash
cd /home/frm/Documentos/wks/spring-saas-core
./scripts/smoke.sh
```

O script valida: health, OpenAPI, token de dev, `/v1/me`, tenants/policies/flags CRUD, auditoria, snapshot, cenário ABAC deny.

---

## 2. node-b2b-orders

Requer `.env` (o `up.sh` copia de `.env.example` se não existir). O build já usa `network: host` para evitar falhas de DNS no Prisma/npm.

```bash
cd /home/frm/Documentos/wks/node-b2b-orders
./scripts/up.sh
./scripts/migrate.sh
./scripts/seed.sh
```

URLs: API http://localhost:3000 | Docs http://localhost:3000/docs | RabbitMQ http://localhost:15673 | Grafana http://localhost:3001.

### Testar (smoke)

O smoke do orders espera que o **RabbitMQ do spring-saas-core** (porta **5675**) esteja acessível em `localhost` para o passo “payment.settled → order PAID”. Ou seja, suba o spring-saas-core antes.

```bash
cd /home/frm/Documentos/wks/node-b2b-orders
./scripts/smoke.sh
```

Se a API já estiver no ar e você não quiser que o script suba de novo a stack:

```bash
SKIP_UP=1 ./scripts/smoke.sh
```

---

## 3. py-payments-ledger

```bash
cd /home/frm/Documentos/wks/py-payments-ledger
./scripts/up.sh
```

O script já sobe os containers e roda as migrações (Alembic). URLs: API http://localhost:8000 | Docs http://localhost:8000/docs | RabbitMQ http://localhost:15674 | Grafana http://localhost:3002.

### Testar (smoke)

```bash
cd /home/frm/Documentos/wks/py-payments-ledger
./scripts/smoke.sh
```

---

## 4. fluxe-b2b-suite (UI)

### Comunicação front ↔ backends

Em **modo dev** (`pnpm run dev` / `nx serve`), o front usa **proxy** para falar com as APIs sem CORS:

- Requisições para `/api/core` → proxy para `http://localhost:8080`
- Requisições para `/api/orders` → proxy para `http://localhost:3000`
- Requisições para `/api/payments` → proxy para `http://localhost:8000`

O `config.json` em `public/assets/` está com essas URLs relativas. **Suba Core (8080) e Orders (3000)** antes de abrir o front; Payments (8000) é opcional se não for usar telas de pagamento.

### Desenvolvimento local (recomendado para testar contra os backends no host)

Os apps da suíte apontam para:

- **Core:** http://localhost:8080  
- **Orders:** http://localhost:3000  
- **Payments:** http://localhost:8000  

Instale dependências e suba o ops-portal (ou outro app):

```bash
cd /home/frm/Documentos/wks/fluxe-b2b-suite
pnpm install
pnpm run dev
# ou: cd saas-suite-ui && pnpm nx serve ops-portal
```

Outros comandos: `pnpm run dev:shop`, `pnpm run dev:admin`.

### Docker (UIs buildadas)

Se quiser rodar as UIs em containers (apontando para host):

```bash
cd /home/frm/Documentos/wks/fluxe-b2b-suite/saas-suite-ui
docker compose -f docker-compose.dev.yml up -d --build
```

Admin-console: http://localhost:4200 | Ops-portal: http://localhost:4300 (variáveis já configuradas para host.docker.internal:8080, 3000, 8000).

---

## Resumo rápido: um terminal por projeto

| Projeto              | Comando (resumido)                                      | Porta principal |
|---------------------|---------------------------------------------------------|-----------------|
| spring-saas-core    | `./scripts/up.sh && ./scripts/seed.sh`                  | 8080            |
| node-b2b-orders     | `./scripts/up.sh && ./scripts/migrate.sh && ./scripts/seed.sh` | 3000            |
| py-payments-ledger  | `./scripts/up.sh`                                       | 8000            |
| fluxe-b2b-suite     | `pnpm install && pnpm run dev`                          | 4200/4300 (conforme app) |

---

## Testes de fumaça (checklist)

1. **spring-saas-core:** `./scripts/smoke.sh` (no diretório do projeto).
2. **node-b2b-orders:** `./scripts/smoke.sh` (com Core e RabbitMQ do Core acessíveis; opcionalmente `SKIP_UP=1` se a stack já estiver no ar).
3. **py-payments-ledger:** `./scripts/smoke.sh`.

Se os três smoke passarem, a stack está operacional para uso pela UI e para integrações (JWT, outbox, etc.).

---

## Troubleshooting

- **Rede no build Docker:** Se aparecer `Unknown host` ou `getaddrinfo EAI_AGAIN` no build, use a **Opção B** do spring-saas-core (infra no Docker, app no host) e confira se o `docker-compose.yml` do node-b2b-orders tem `network: host` nos builds da `api` e do `worker`.
- **Maven wrapper (Spring):** Se der “nenhum atributo de manifesto principal” no `mvnw`, use a Opção A (Docker com `network: host` no build) ou reinstale o wrapper: `mvn -N wrapper:wrapper`.
- **RabbitMQ:** O smoke do node-b2b-orders publica `payment.settled` no RabbitMQ do **spring-saas-core** (porta 5675 em localhost). Garanta que o Core (e seu RabbitMQ) estejam rodando antes do smoke do orders.
