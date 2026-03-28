# Assistente IA no Admin (governança)

O **spring-saas-core** expõe `/v1/ai/*` (chat, insights, análise de auditoria, recomendações). O **Admin Console** consome estes endpoints na página do assistente.

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

## Permissões (ABAC)

Os endpoints de IA exigem **`analytics:read`**. O seed essencial (`008-essential-seed-all-envs`) inclui política que permite esta permissão; utilizadores com roles/políticas de admin devem conseguir aceder.

## Checklist rápido (Railway staging)

1. Serviço **spring-saas-core** → **Variables** → `OPENAI_API_KEY` = `sk-...` (ou equivalente).
2. Manter `SPRING_PROFILES_ACTIVE=staging` (ou `AI_ENABLED=true` se usares outro profile).
3. Redeploy do Core.
4. No Admin, abrir a página do assistente e confirmar que o estado mostra **LLM** (não só “Rule Engine”).

## Referência de código

- `com.union.solutions.saascore.config.AiConfig` — `isEnabled()` = enabled **e** chave não vazia.
- `com.union.solutions.saascore.application.service.AiService` — chamadas ao modelo e fallbacks.
