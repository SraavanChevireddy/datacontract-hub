# Changelog

All notable changes to this project are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-06-23

### Added
- Initial public release.
- Compatibility engine supporting `BACKWARD`, `FORWARD`, `FULL`, and `NONE` modes
  with nested struct support and type-widening rules.
- Contract / version / audit domain model backed by PostgreSQL (Flyway migrations).
- Governance workflow: propose, approve, reject, with compatibility enforced on submit.
- CI-friendly dry-run `/check` endpoint.
- REST API with OpenAPI/Swagger UI, RFC-7807 error responses.
- Docker + docker-compose for local Postgres + service.
