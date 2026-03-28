# Assistente IA no Admin (governança)

O **spring-saas-core** expõe `/v1/ai/*` (chat, insights, análise de auditoria, recomendações). O **Admin Console** consome estes endpoints na página do assistente.

## Utilizador seed com role **admin** (Felipe Ricarte)

O Liquibase **`008-essential-seed-all-envs`** cria o utilizador **`felipericartem@gmail.com`** (nome **Felipe Ricarte Magalhães**) no tenant plataforma (`00000000-0000-0000-0000-000000000001`) com **`roles = admin`**. O changeset **`017-ensure-felipe-ricarte-admin-role`** corrige bases já existentes em que essa role tenha sido alterada (reaplica `admin`).

Com **admin**, o JWT inclui permissões compatíveis com **`analytics:read`**, necessária para `/v1/ai/*`.

## Quando o LLM está ativo

O código usa `AiProperties`: o modelo **só é chamado** se `app.ai.enabled` **e** `OPENAI_API_KEY` estiverem definidos (não vazios). Caso contrário, respostas são **baseadas em regras** (badge “Rule Engine” no front).

### Variáveis (Railway ou `.env`)

| Variável | Descrição |
|----------|-----------|
| `AI_ENABLED` | `true` para permitir uso do LLM (junto com a chave). Em **staging** o default do profile é `true` via `application-staging.yml`. |
| `OPENAI_API_KEY` | Chave da API (OpenAI ou compatível com `POST /v1/chat/completions`). |
| `AI_BASE_URL` | Opcional; default `https://api.openai.com/v1`. |
| `AI_MODEL` | Opcional; default `gpt-4o-mini`. |

Exemplos: `railway.staging.env.example`, `railway.prod.env.example`.

**Local (Docker Compose do Core):** o serviço `app` já lê `AI_ENABLED` e `OPENAI_API_KEY` do ambiente do host. Exemplo antes de subir:

```bash
export AI_ENABLED=true
export OPENAI_API_KEY=sk-...
# na pasta spring-saas-core
docker compose up -d
```

Sem `OPENAI_API_KEY`, o Core continua em **Rule Engine** mesmo com `AI_ENABLED=true`.

## Diagnosticar “Rule Engine” no Admin

1. Chamar **`GET /v1/ai/status`** (com o mesmo `Authorization` que o Admin usa). O JSON inclui:
   - **`engine`**: `llm` ou `rule-engine`
   - **`aiEnabledProperty`**: valor de `AI_ENABLED` lido pela app
   - **`openaiKeyConfigured`**: se existe chave não vazia no processo (não mostra o segredo)

2. Se o **GET falhar** (403, rede, proxy), o front pode assumir Rule Engine por defeito — verificar consola de rede e ABAC (`analytics:read`).

## Permissões (ABAC)

Os endpoints de IA exigem **`analytics:read`**. O seed essencial inclui política que permite esta permissão; utilizadores com role **admin** (como o seed acima) devem conseguir aceder.

## Checklist rápido (Railway staging)

1. Serviço **spring-saas-core** → **Variables** → `OPENAI_API_KEY` = `sk-...` (ou equivalente).
2. Manter `SPRING_PROFILES_ACTIVE=staging` (ou `AI_ENABLED=true` se usares outro profile).
3. Redeploy do Core.
4. `GET /v1/ai/status` → `engine: "llm"` e `openaiKeyConfigured: true`.
5. No Admin, abrir a página do assistente e confirmar o badge **LLM**.

## Referência de código

- `com.union.solutions.saascore.config.AiConfig` — `isEnabled()` = enabled **e** chave não vazia.
- `com.union.solutions.saascore.application.service.AiService` — chamadas ao modelo e fallbacks.
- `com.union.solutions.saascore.adapters.in.rest.AiController` — `/v1/ai/status` e encaminhamento do chat para o LLM quando configurado.
