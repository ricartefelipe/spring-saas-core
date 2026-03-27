# Política de versão de contratos (eventos, cabeçalhos, identidade, JSON Schema)

**Fonte canónica:** este repositório (`spring-saas-core`), pasta `docs/contracts/`.  
**Consumidores:** espelhos obrigatórios em `node-b2b-orders` e `py-payments-ledger` (validação: `fluxe-b2b-suite/scripts/check-contract-drift.sh`).

## Âmbito

- Ficheiros: `events.md`, `headers.md`, `identity.md`, `schemas/*.json` publicados pelo Core.
- API HTTP `/v1/*` segue versionamento de rota e OpenAPI à parte; este documento foca **contratos de integração** (mensagens, headers, JWT).

## Alterações compatíveis (minor, sem bump obrigatório de major)

- Novos valores opcionais em payloads ou novos tipos de evento **aditivos** (consumidores antigos ignoram campos desconhecidos).
- Novos ficheiros de schema **não** usados por consumidores existentes.
- Clarificações apenas em documentação sem alterar semântica já implementada.

**Processo:** PR no Core → merge em `develop` → espelhar nos consumidores no mesmo ciclo ou no PR seguinte → `verify:contracts` verde.

## Alterações incompatíveis (major)

- Remoção ou renomeação de campos obrigatórios, mudança de tipo, alteração de semântica de claims JWT, mudança de routing keys que quebra consumidores.
- Novo schema com `$id` que substitui versão anterior (ex.: `/v1` → `/v2`).

**Processo obrigatório:**

1. Documentar breaking change em `CHANGELOG.md` do Core e abrir RFC breve no PR.
2. Incrementar versão em `$id` dos JSON Schema quando aplicável (ex.: `.../v2`).
3. Alinhar `node-b2b-orders` e `py-payments-ledger` **antes** ou no mesmo PR que o Core, ou em janela acordada (ver abaixo).
4. Nunca mergear `develop` em `master` em consumidor sem o Core compatível em produção/staging.

## Janela de compatibilidade

- Após merge em `develop` no Core, os consumidores devem absorver o espelho **no máximo em 2 ciclos de integração** (ex.: duas semanas ou dois merges a `develop`, o que for menor), salvo exceção explícita no PR.
- Em staging, **drift zero** é obrigatório antes de promover conjunto a produção.

## Referência nos consumidores

- `node-b2b-orders/docs/contracts/` e `py-payments-ledger/docs/contracts/` mantêm cópia byte-a-byte dos ficheiros canónicos; esta política aplica-se por igual aos espelhos.
