#!/usr/bin/env bash
# Testa a API do Resend antes de configurar no Railway.
# Uso: RESEND_API_KEY=re_xxx EMAIL_FROM=noreply@mail.seudominio.com ./scripts/test-resend.sh seu@email.com
# Ou com nome: EMAIL_FROM_NAME="Fluxe B2B Suite" EMAIL_FROM=... ./scripts/test-resend.sh seu@email.com

set -e

API_KEY="${RESEND_API_KEY:?Defina RESEND_API_KEY}"
FROM="${EMAIL_FROM:?Defina EMAIL_FROM}"
FROM_NAME="${EMAIL_FROM_NAME:-}"
TO="${1:?Uso: $0 destinatario@email.com}"

if [ -n "$FROM_NAME" ]; then
  FROM="$FROM_NAME <$FROM>"
fi

echo "Enviando teste: from=$FROM to=$TO ..."
PAYLOAD=$(printf '{"from":"%s","to":["%s"],"subject":"Teste Resend - Fluxe B2B Suite","html":"<p>Se recebeu isto, o Resend está OK.</p>"}' "$FROM" "$TO")
RESP=$(curl -s -w "\n%{http_code}" -X POST "https://api.resend.com/emails" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD")

BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -1)

if [ "$CODE" = "200" ] || [ "$CODE" = "201" ]; then
  echo "OK — Resend aceitou (HTTP $CODE). Verifique a caixa de $TO"
  echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
  exit 0
else
  echo "ERRO — Resend retornou HTTP $CODE"
  echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
  echo ""
  echo "Dicas: 403 = domínio não verificado ou EMAIL_FROM não bate. Vá em resend.com/domains"
  exit 1
fi
