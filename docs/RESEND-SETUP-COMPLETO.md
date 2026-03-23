# Checklist Resend — 7 passos para resolver 403

Execute na ordem. Ao terminar o passo 4 no Resend, use o passo 5 para configurar o Railway.

---

## 1. Resend: adicionar domínio

1. Acesse [resend.com/domains](https://resend.com/domains)
2. **Add Domain** → ex.: `mail.fluxe.com.br`

## 2. DNS: SPF e DKIM

No painel do Resend, copie os registros **SPF (TXT)** e **DKIM (TXT)**.

Adicione no seu provedor DNS (Registro.br, Cloudflare, Hostinger, etc.).

Aguarde propagação (5–30 min).

## 3. Resend: verificar

Em [resend.com/domains](https://resend.com/domains), clique em **Verify DNS Records**.

Aguarde status **Verified** (verde).

## 4. Resend: API Key

1. Acesse [resend.com/api-keys](https://resend.com/api-keys)
2. **Create API Key** → nome `fluxe-staging`
3. Copie a key (começa com `re_`)

## 5. Script único: configurar Railway + testar

No terminal, a partir de `spring-saas-core`:

```bash
# Defina suas credenciais (ou exporte antes)
export RESEND_API_KEY=re_sua_key_aqui
export EMAIL_FROM=noreply@mail.fluxe.com.br   # domínio EXATO verificado no passo 1
export EMAIL_FROM_NAME="Fluxe B2B Suite"

# Configure Railway
railway variable set EMAIL_PROVIDER=resend
railway variable set RESEND_API_KEY=$RESEND_API_KEY
railway variable set EMAIL_FROM=$EMAIL_FROM
railway variable set EMAIL_FROM_NAME="$EMAIL_FROM_NAME"

# Teste
./scripts/test-resend.sh seu@email.com
```

Se o teste retornar **HTTP 200/201**, o Resend está OK.

## 6. Redeploy

```bash
railway up --detach
```

## 7. Validar em produção

Envie um convite de usuário pelo Admin Console. O e-mail deve chegar na caixa do destinatário.

---

## Se ainda der 403

| Sintoma | Causa | Ação |
|---------|-------|------|
| `domain is not verified` | DNS não propagou ou incompleto | Repetir passo 2 e 3 |
| `domain mismatch` | EMAIL_FROM não bate com domínio | EMAIL_FROM = `noreply@` + domínio exato |
| 401 | API key incorreta | Gerar nova key no passo 4 |
