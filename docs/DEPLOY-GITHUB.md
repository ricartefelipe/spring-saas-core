# Deploy via GitHub (Actions + GHCR)

A publicação da imagem Docker para o **GHCR** é feita pelos workflows do GitHub Actions.

## Workflows

| Workflow | Ficheiro | Quando corre | O que faz |
|----------|----------|--------------|-----------|
| **CI** | `.github/workflows/ci.yml` | Push/PR em `develop` e `master` | Build Maven, Spotless, verificação OpenAPI |
| **Build & Push Docker** | `.github/workflows/build-push.yml` | Push em `develop` e `master`, ou **Run workflow** manual | `./mvnw test`, build da imagem (`docker/app.Dockerfile`), push para **GHCR** com tags `develop`, `master`, `latest` (default branch) e SHA |

## Imagem no GHCR

- Repositório da imagem: `ghcr.io/<owner>/spring-saas-core`
- O VPS e o `docker-compose.prod.yml` do **fluxe-b2b-suite** fazem `docker pull` dessa imagem (variável `GHCR_ORG` + tag `latest` ou `SAAS_CORE_TAG`).

## Requisitos na conta GitHub

1. **GitHub Actions** com minutos disponíveis e **billing** em dia (pagamento / spending limit).
2. Permissões do workflow **Build & Push**: `packages: write` (já definido no YAML).
3. Para disparar manualmente: **Actions** → **Build & Push Docker Image** → **Run workflow**.

## Fluxo típico

1. Merge para `develop` → CI + imagem com tag `develop`.
2. Merge `develop` → `master` → CI + imagem com tags `master` e `latest`.
3. No **fluxe-b2b-suite**, o deploy de produção no VPS (quando aplicável) puxa `spring-saas-core:latest` após o push em `master` — ver `docs/DEPLOY-GITHUB.md` nesse repositório.
