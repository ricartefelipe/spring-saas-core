# Assistente IA no Admin (governança)

O **spring-saas-core** expõe `/v1/ai/*` (chat, insights, análise de auditoria, recomendações). O **Admin Console** consome estes endpoints na página do assistente.

## Permissões e role **admin**

Os endpoints de IA precisam de **`analytics:read`**. Quem tem role **`admin`** (e políticas ABAC alinhadas) costuma ter essas permissões no JWT.

### Repor `admin` no utilizador (acidente na BD)

Não há migração no repositório para um email concreto. Se alteraste **`roles`** na tabela **`users`** e perdeste o privilégio, repõe **manualmente** no Postgres (ajusta o email):

```sql
UPDATE users
SET roles = 'admin', updated_at = CURRENT_TIMESTAMP
WHERE email = 'teu-email@exemplo.com';
```

(Roles são uma lista separada por vírgulas, ex.: `admin` ou `admin,ops` — ver coluna `roles` no teu caso.)

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

### Badge **LLM** mas o chat ainda parece “só regras”

Se **`GET /v1/ai/status`** indica modelo (ex.: `gpt-4o-mini`) mas a resposta do **chat** não é conversa natural, o mais provável é a **chamada à API OpenAI ter falhado** (rede, quota, 401, timeout, circuito Resilience4j). O `/status` só verifica configuração; o **chat** chama o modelo em tempo real. Ver **logs do Core** no Railway e o painel da OpenAI. A partir do código atual, o fallback deixa explícito que a chave está configurada mas o pedido ao modelo falhou — não confundir com “falta `OPENAI_API_KEY`”.

## Diagnosticar “Rule Engine” no Admin

1. Chamar **`GET /v1/ai/status`** (com o mesmo `Authorization` que o Admin usa). O JSON inclui:
   - **`engine`**: `llm` ou `rule-engine`
   - **`aiEnabledProperty`**: valor de `AI_ENABLED` lido pela app
   - **`openaiKeyConfigured`**: se existe chave não vazia no processo (não mostra o segredo)

2. Se o **GET falhar** (403, rede, proxy), o front pode assumir Rule Engine por defeito — verificar consola de rede e ABAC (`analytics:read`).

O seed essencial inclui políticas que permitem **`analytics:read`** onde aplicável.

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
