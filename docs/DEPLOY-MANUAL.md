# Deploy manual da imagem (sem GitHub Actions)

Quando o workflow **Build & Push** não corre (ex.: billing), publique a imagem a partir da sua máquina.

## Passos

1. Login no GHCR:
   ```bash
   docker login ghcr.io -u <seu-user-github>
   ```
2. Na raiz deste repositório:
   ```bash
   chmod +x scripts/build-push-image.sh
   ./scripts/build-push-image.sh
   ```

Isto executa `./mvnw package`, `docker build -f docker/app.Dockerfile.hostbuild` e `docker push` para `ghcr.io/ricartefelipe/spring-saas-core:latest` (ou `IMAGE=...` para outra tag).

## Deploy no servidor

O sync e o `deploy.sh` no VPS estão documentados no repositório **fluxe-b2b-suite**: `docs/DEPLOY-MANUAL.md`.

## Ver também

- [DEPLOY-GITHUB.md](DEPLOY-GITHUB.md) — quando usar Actions.
