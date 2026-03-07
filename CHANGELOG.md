# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
