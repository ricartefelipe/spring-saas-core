# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.2] - 2026-11-04

### Security
- a imagem aplica as correções do Alpine na etapa final: libexpat 2.9.0, com duas falhas altas publicadas em 03/11/2026, passa à 2.10.0.
- Prometheus 3.15.1 e Grafana 12.4.14 fixados por digest.

### Fixed
- a API anuncia a versão do `pom.xml` (antes 1.0.0); um teste prende a versão ao CHANGELOG.
- o provisionamento do Grafana volta a carregar fontes de dados e painéis.

## [1.4.1] - 2026-11-03

### Changed
- RabbitMQ 4.4.0 no ambiente local. O outbox publica em exchange durável pelo Spring AMQP, compatível com o RabbitMQ 4.

## [1.4.0] - 2026-11-03

### Security
- atualiza para o Spring Boot 4, corrige falhas críticas e altas nas dependências e na imagem, e ajusta três comportamentos que a atualização alterava: inicialização com rastreamento vazio, leitura de JSON com campo numérico ausente e exigência de configuração.

## [1.3.5] - 2026-11-03

### Security
- Spring Boot 3.2.5 → 4.0.9 (Spring Framework 7, Spring Security 7, Tomcat 11) e amqp-client 5.34.0: a imagem sai de 55 falhas críticas e altas, 11 críticas, para nenhuma.
- Isolamento entre locatários: o administrador de um locatário só vê e lê o próprio. Criar, alterar e apagar locatários, editar políticas globais, métricas, analytics e o assistente passam a ser exclusivos da plataforma; auditoria, snapshot, saúde e flags ficam presas ao locatário do token.

### Changed
- Rastreamento pelo `spring-boot-starter-opentelemetry`; propriedades em `management.opentelemetry.tracing.export.otlp.*` e `management.tracing.export.enabled`. Endpoint OTLP vazio desliga só a exportação.
- JSON da API no Jackson 3 com `spring.jackson.use-jackson2-defaults`, mantendo o contrato anterior.
- Tokens de administrador de locatário deixam de trazer `tenants:write` e `policies:write`.

### Fixed
- `GET /v1/tenants/{id}/health` respondia 500 para locatário sem login registrado.

## [1.3.4] - 2026-11-02

### Removed
- Scripts sem uso ou quebrados: `chaos-demo.sh` (rotas inexistentes), `generate-screenshots.sh` (imagens de preenchimento), `up-kafka.sh` (o projeto não usa Kafka), `smoke-post-merge.sh` e `portfolio-up.sh`.
- Backlog interno `docs/BACKLOG-EVOLUCAO.md`.

### Changed
- Contato do projeto passa a ser `contato@unionsolutions.tech`.

## [1.3.3] - 2026-11-02

### Security
- Produção sem carga de demonstração deixa de criar `admin@local`, `ops@demo.example.com` e as contas da carga 009 com senha publicada; a migração 027 desativa essas senhas em bancos existentes, salvo contas cuja senha já foi trocada.
- A carga inicial não cria mais a conta com e-mail pessoal. Em instalação nova, o primeiro administrador vem de `APP_BOOTSTRAP_ADMIN_EMAIL` e `APP_BOOTSTRAP_ADMIN_PASSWORD`, com troca de senha obrigatória.

### Fixed
- Os contextos `prod,seed` do perfil de piloto voltam a funcionar em banco novo.

## [1.3.2] - 2026-10-29

### Fixed
- `scripts/up.sh` constrói o jar antes do `docker compose` (a imagem local copia `target/*.jar`) e falha claramente se ele não existir.
- `scripts/smoke.sh` aceita `SMOKE_BASE_URL`, espera a aplicação com prazo, confere pelo título do contrato em `/v3/api-docs` que está medindo a Spring SaaS Core API e imprime diagnóstico ao falhar.
- `container_name` fixo do RabbitMQ removido do compose (colidia com outra pilha).
- CI: o gate de build propaga o status real do Maven (`PIPESTATUS`) em vez do `tee`.

## [1.0.0] - 2026-03-07

### Added
- Multi-tenant control plane with CRUD for tenants, policies and feature flags
- ABAC/RBAC governance engine with DENY-precedent and default-deny
- JWT standardized with tenant snapshot for downstream services (orders, payments)
- Outbox pattern with RabbitMQ for reliable event publishing
- Audit log with retention policies, streaming export and consultable API
- OIDC audience validation for production environments
- Liquibase seed data for local development profile
- Prometheus metrics endpoint (`/actuator/prometheus`) with custom counters
- Grafana dashboards: overview, outbox, feature flags, circuit breakers, rate limit, auth/audit
- CI/CD pipeline with GitHub Actions and multi-arch Docker build (amd64 + arm64)
- Docker image published to GHCR
- OpenAPI documentation for v1 API
- Health checks and readiness probes

### Changed
- Refactored controllers, services, repositories and tests for cleaner architecture
- Extracted duplicated `publishOutbox` into `OutboxPublisherPort` to reduce code smells
- Docker Compose default ports adjusted to avoid conflicts with other B2B services

### Fixed
- ABAC enforcement added to snapshot controller
- PolicyControllerTest coverage gaps addressed

### Security
- Production hardening: OIDC, ABAC on all sensitive endpoints, no credentials in code
- Audit logging for ACCESS_DENIED events
- Default-deny policy enforcement across all API routes
