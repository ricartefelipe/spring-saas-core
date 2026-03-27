# Runbook de rollback - spring-saas-core

## Gatilhos

- erro 5xx acima do baseline por 10 minutos
- falha de autenticacao/autorizacao em fluxo critico
- inconsistencia de contratos para orders/payments

## Procedimento

1. Pausar novas promocoes para `develop`/`master`
2. Identificar ultimo artefato/tag estavel em producao
3. Reverter deploy para a imagem/tag anterior
4. Validar `/actuator/health` e `/actuator/prometheus`
5. Validar endpoint de identidade (`/v1/me`) e snapshot tenant
6. Confirmar publicacao de eventos de tenant/policy/flag

## Validacao pos-rollback

- `./scripts/smoke.sh` no ambiente de validacao
- logs sem erro critico e fila RabbitMQ sem backlog anormal
- incidente atualizado com causa raiz e plano preventivo
