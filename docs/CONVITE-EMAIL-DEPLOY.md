# E-mail de convite — Fluxe B2B Suite e troca obrigatória de senha

## O e-mail “não chega” — causa n.º 1

O **padrão** no Core é `EMAIL_PROVIDER=log` (`application.yml`). **Variável vazia** (`EMAIL_PROVIDER=` sem valor) é tratada como **`log`** — do contrário a API não devolvia `temporaryPassword` e parecia “travado”. Nesse modo **nenhum e-mail sai do servidor**; o convite aparece no **Admin** (senha no diálogo) e nos **logs**. Para envio real:

1. No serviço **spring-saas-core** (Railway ou outro): `EMAIL_PROVIDER=resend`, `RESEND_API_KEY`, `EMAIL_FROM` (domínio **já verificado** no Resend — ver secção abaixo).
2. Com `log`, o **Admin Console** mostra um diálogo com a **senha temporária** após convidar (a API devolve `temporaryPassword` no JSON). Também pode ver nos **logs** do Core a linha `Convite criado; EMAIL_PROVIDER=log`.

3. **Resend/SMTP com envio aceite (HTTP 2xx / SMTP OK):** a API **omite** `temporaryPassword` no JSON (a senha não transita pela resposta).

4. **Resend/SMTP em falha com `EMAIL_FAIL_ON_DELIVERY_ERROR=false` (fail-soft):** o utilizador **é criado** e a API **devolve `temporaryPassword`** para o admin repassar manualmente — o mesmo critério de “mostrar senha” que em `log`, mas só quando o fornecedor **tentou** enviar e **não** aceitou a mensagem.

Se já usa `resend` e mesmo assim não chega: caixa de **spam**, domínio/DNS não verificado (403 no Resend), ou `EMAIL_FROM` diferente do subdomínio verificado.

### Railway / PaaS: SMTP vs Resend

- **Resend** usa **HTTPS** (porta 443) — costuma **funcionar** em Railway e similares.
- **SMTP** (portas 25, 465, 587) é **ligação TCP direta** ao servidor de correio. **Muitos** fornecedores de app hosting **bloqueiam ou restringem** saída SMTP para combater spam. Se configuraste `EMAIL_PROVIDER=smtp` e os logs mostram timeout ou “connection refused”, **muda para `EMAIL_PROVIDER=resend`** com domínio verificado.

### Como saber o que o Core está a usar

Ao **arrancar**, o Core regista uma linha como:

`=== E-mail (arranque) === EMAIL_PROVIDER_raw=resend | providerEfetivo=resend | RESEND_API_KEY preenchida=true | ...`

Consulta os **Deploy Logs** do serviço no Railway. Se `providerEfetivo=log`, **não há envio real**. Se `resend` e `RESEND_API_KEY preenchida=false`, o envio vai falhar.

**Typo no nome da variável ou no valor:** só existem `resend`, `smtp` e `log`. Qualquer outro valor (ex.: `resnd`) é tratado como **`log`**; no arranque aparece **ERROR** a explicar. O nome da variável tem de ser exactamente **`EMAIL_PROVIDER`** (não `APP_EMAIL_PROVIDER`, etc.).

---

## Enviar por SMTP (em vez do Resend)

O Core também aceita **`EMAIL_PROVIDER=smtp`**: envio por **SMTP clássico** (Gmail com senha de app, SendGrid SMTP, Amazon SES, Mailgun, Postfix, etc.). Não é obrigatório usar o Resend.

**Maiúsculas/minúsculas:** `EMAIL_PROVIDER=SMTP`, `smtp` ou `Smtp` são aceites (normalização interna). Antes, só `smtp` minúsculo ativava o bean — um valor `SMTP` no painel podia **não carregar** o enviador.

1. No Railway (ou `.env`):  
   - `EMAIL_PROVIDER=smtp`  
   - `SMTP_HOST` — ex.: `smtp.gmail.com`, `smtp.sendgrid.net`  
   - `SMTP_PORT` — **587** (STARTTLS, padrão) ou **465** (SSL implícito: o Core aplica SSL automaticamente na porta 465, ou use `SMTP_SSL_ENABLED=true` na 587 se o provedor exigir)  
   - `SMTP_USER` / `SMTP_PASSWORD` — credenciais do provedor  
   - `EMAIL_FROM` / `EMAIL_FROM_NAME` — o remetente que o servidor SMTP autoriza  
2. O remetente (`EMAIL_FROM`) deve ser **permitido** pela conta SMTP (e, em produção, ter **SPF/DKIM** no DNS do domínio, como em qualquer e-mail transacional).
3. Redeploy do `spring-saas-core`.

**Nota:** Com `smtp` ou `resend`, a API **só** omite `temporaryPassword` quando o envio foi **aceite** pelo provedor. Em falha de entrega com fail-soft (`EMAIL_FAIL_ON_DELIVERY_ERROR=false`), a resposta pode incluir `temporaryPassword` como em modo `log`.

---

## Usar Resend já (checklist rápido)

1. [resend.com/domains](https://resend.com/domains) → **Add Domain** (ex.: `mail.fluxe.com.br`)
2. Configure **SPF** e **DKIM** no DNS do domínio
3. Clique em **Verify DNS Records** até status `verified`
4. [resend.com/api-keys](https://resend.com/api-keys) → crie uma API key
5. No **Railway** (spring-saas-core) → Settings → Variables:
   - `EMAIL_PROVIDER` = `resend`
   - `RESEND_API_KEY` = `re_xxx` (sua key)
   - `EMAIL_FROM` = `noreply@mail.fluxe.com.br` (domínio **exato** verificado)
   - `EMAIL_FROM_NAME` = `Fluxe B2B Suite`
6. Teste localmente: `RESEND_API_KEY=re_xxx EMAIL_FROM=noreply@mail.fluxe.com.br ./scripts/test-resend.sh seu@email.com`
7. Redeploy do spring-saas-core no Railway

---

## Como liberar e-mails no Resend (documentação verificada)

Com base na [documentação oficial do Resend](https://resend.com/docs), para enviar para destinatários reais é necessário seguir estes passos.

### 1) Pare de usar `onboarding@resend.dev` ou `resend.dev`

O domínio `resend.dev` é **só para teste** e só pode enviar para o e-mail da conta Resend. Enviar para terceiros retorna **403**.

- ✅ Use um domínio seu verificado
- ❌ Não use `noreply@resend.dev` ou `onboarding@resend.dev` para clientes reais

### 2) Adicione e verifique seu domínio

1. Acesse [resend.com/domains](https://resend.com/domains)
2. Clique em **Add Domain**
3. **Recomendado:** use um **subdomínio** (ex.: `mail.fluxe.com.br` ou `updates.fluxe.com.br`) — isolamento de reputação e melhor deliverability
4. O Resend exige **SPF (TXT)** e **DKIM (TXT)** no DNS. MX é opcional para Return-Path.
5. Copie os registros DNS e configure no provedor (Registro.br, Cloudflare, Hostinger, etc.)
6. Aguarde propagação e clique em **Verify DNS Records**
7. Status `verified` = pronto para enviar

### 3) Domínio do `from` deve bater exatamente

O `EMAIL_FROM` deve usar **exatamente** o domínio ou subdomínio verificado.

| Verificou no Resend      | EMAIL_FROM correto              | EMAIL_FROM incorreto      |
|--------------------------|----------------------------------|----------------------------|
| `mail.fluxe.com.br`      | `noreply@mail.fluxe.com.br`      | `noreply@fluxe.com.br` ❌  |
| `fluxe.com.br`           | `noreply@fluxe.com.br`           | `noreply@mail.fluxe.com.br`❌ |

Se divergir, dá **403 domain mismatch**. Documentação: [403 Error Domain Mismatch](https://resend.com/docs/knowledge-base/403-error-domain-mismatch).

### 4) API Key e permissões

- Crie em [resend.com/api-keys](https://resend.com/api-keys)
- `full_access` ou `sending_access` — se restrita, confirme se o domínio está autorizado para essa key

### 5) Variáveis no Railway (spring-saas-core)

```bash
EMAIL_PROVIDER=resend
RESEND_API_KEY=re_xxx
EMAIL_FROM=noreply@mail.fluxe.com.br   # domínio EXATO verificado em resend.com/domains
EMAIL_FROM_NAME=Fluxe B2B Suite        # opcional; melhora deliverability
FRONTEND_URL=https://admin-console-staging-b1ab.up.railway.app
```

**Testar Resend antes do deploy:**

```bash
cd spring-saas-core
chmod +x scripts/test-resend.sh
RESEND_API_KEY=re_xxx EMAIL_FROM=noreply@mail.fluxe.com.br ./scripts/test-resend.sh seu@email.com
```

Se retornar HTTP 200/201, o Resend está OK. Se 403, verifique domínio e `EMAIL_FROM` em [resend.com/domains](https://resend.com/domains).

### 6) Quotas e limites

Mesmo com domínio verificado, o Resend aplica limites de taxa, quota diária e mensal. Erros 429 = quota excedida — verifique em [resend.com](https://resend.com) o plano atual.

---

## Workaround imediato: 403 impede cadastro

Se o 403 do Resend **impede o cadastro** de usuários (convite falha), use um destes:

### Opção A — Desativar Resend (usuário criado, senha nos logs)

No Railway, no serviço **spring-saas-core**, defina:

```
EMAIL_PROVIDER=log
```

- Remove `RESEND_API_KEY` ou deixe vazio para evitar confusão
- Com `log`, nenhuma chamada ao Resend é feita; o usuário **é criado**
- A senha temporária aparece nos **Deploy Logs** do Railway (busque por "EMAIL NOT SENT" ou pelo e-mail do usuário)
- Compartilhe a senha manualmente com o novo usuário

### Opção B — Manter Resend, garantir que não propague erro

Confirme que no Railway está definido:

```
EMAIL_FAIL_ON_DELIVERY_ERROR=false
```

Com isso, quando o Resend retornar 403, o usuário **é criado** e o erro é apenas logado. A **senha temporária** pode aparecer na **resposta JSON** do convite (`temporaryPassword`) e nos **logs** do Railway — repasse manualmente ao utilizador.

---

## Checklist rápido para destravar

1. [ ] [resend.com/domains](https://resend.com/domains) → Add Domain (ex.: `mail.seudominio.com`)
2. [ ] Configurar SPF e DKIM no DNS
3. [ ] Verificar até status `verified`
4. [ ] `EMAIL_FROM` = endereço do domínio/subdomínio verificado
5. [ ] `RESEND_API_KEY` correta e com permissão
6. [ ] Redeploy do spring-saas-core após alterar variáveis

---

## Se ainda falha

| Erro / sintoma | Causa provável | Solução |
|----------------|----------------|---------|
| 403 + `resend.dev` | Enviando para terceiros com domínio de teste | Verificar domínio e trocar `from` |
| 403 + `domain is not verified` | Domínio do `from` não verificado | Verificar em resend.com/domains |
| 403 domain mismatch | `from` não bate com domínio verificado | Igualar `EMAIL_FROM` ao domínio/subdomínio verificado |
| 401 / 403 | API key incorreta ou restrita | Gerar nova key ou conferir permissões |
| 429 | Quota diária/mensal excedida | Upgrade de plano ou aguardar reset |

## Comportamento atual (a partir de `develop` após PR #42)

- **Nome no e-mail:** Para o tenant de plataforma (UUID `00000000-0000-0000-0000-000000000001`), o convite mostra sempre **"Fluxe B2B Suite"**, nunca "System".
- **Troca de senha:** O texto do e-mail deixa claro que é **obrigatório** alterar a senha no primeiro acesso; o login retorna `must_change_password` e o JWT inclui o claim `mcp` para o front bloquear o uso da app até a troca.

## Se ainda recebes "System" ou "Recomendamos alterar a senha"

Isso indica que o **servidor que envia os e-mails está a correr uma versão antiga** do spring-saas-core.

### O que fazer

1. **Staging (branch `develop`)**  
   - No Railway, o deploy do spring-saas-core deve ser feito a partir da branch **`develop`**.  
   - Confirma em **Settings → Source** que a **Production Branch** do serviço spring-saas-core é `develop`.  
   - Se já estiver correto, dispara um **redeploy** (ex.: "Redeploy" no dashboard ou novo push em `develop`).  
   - Garante que o **Admin Console** (e qualquer front que use convites) está a usar a **URL do Core** desse ambiente (staging), não de outro.

2. **Produção (branch `master`)**  
   - Faz **merge de `develop` em `master`** no repositório spring-saas-core.  
   - Depois do deploy em produção, o Core de produção passará a enviar o e-mail novo.

3. **Local**  
   - Corre o JAR ou `mvn spring-boot:run` a partir do código atual em `develop` (ou `master` após merge).

Após o deploy da versão correta, **novos convites** passam a sair com "Fluxe B2B Suite" e com o texto de obrigatoriedade de troca de senha. Utilizadores já convidados antes podem usar **"Reenviar convite"** no Admin para receber o e-mail actualizado e o fluxo de troca obrigatória.

## Troca obrigatória de senha no primeiro uso

**O que fazemos hoje**

1. No convite, o utilizador é criado com `must_change_password = true`.
2. No login, a API devolve `must_change_password` e o JWT inclui o claim `mcp`.
3. O front (ops-portal / admin-console) redireciona para `/change-password` e o **guard** impede navegar no resto da app até trocar a senha.
4. Após trocar, o Core devolve um **novo token** sem `mcp`.

**Sugestões extra (opcional, futuro)**

- Expirar senhas temporárias após N dias (obrigar reenvio de convite).
- Notificar por e-mail se alguém tentar entrar várias vezes sem completar a troca.

Se alguém convidado **antes** deste fluxo ainda entra sem ser forçado a trocar: usar **Reenviar convite** ou corrigir o flag na base de dados.

## Testes

- **`EmailTemplatesTest`** — Garante que o corpo do e-mail de convite mostra "Fluxe B2B Suite" quando o nome do tenant é "System", "Sistema" ou vazio; e mostra o nome real (ex.: "Acme Distribuidora") nos outros casos. Inclui cenário com senha temporária (bloco "Obrigatório").
- **`UserManagementUseCaseTest`** — Garante que `invite()` envia e-mail com assunto e corpo contendo "Fluxe B2B Suite" quando o tenant é o de plataforma ou o nome na BD é "System"/"Sistema"; e com nome do tenant nos outros casos. Inclui testes de utilizador já existente e de `mustChangePassword = true` no convite.
