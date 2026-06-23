# datacontract-hub

[![CI](https://github.com/SraavanChevireddy/datacontract-hub/actions/workflows/ci.yml/badge.svg)](https://github.com/SraavanChevireddy/datacontract-hub/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)

**A governance service for data contracts.** Register a schema, declare the compatibility tier it must hold, and every proposed change is checked, versioned, approved, and audited — before it ever reaches a topic, table, or downstream consumer.

> Status: **early-stage (v0.1).** The compatibility engine and governance workflow are real and fully tested. It is a foundation to build on, not a finished product — see [Roadmap](#roadmap).

---

## The problem

In most data platforms, a schema is whatever the last producer deployed. There's no contract, no gate, and no record of who changed what. A field quietly goes from `LONG` to `INT`, a required column disappears, and three teams downstream find out in production at 2am.

Schema registries solve part of this for a single serialization format. **datacontract-hub** is format-agnostic and adds the things governance actually needs:

- **Compatibility enforcement** — `BACKWARD`, `FORWARD`, `FULL`, or `NONE`, checked on every change.
- **An approval workflow** — changes are *proposed*, then explicitly approved or rejected by an owner.
- **An append-only audit trail** — every action (created, proposed, approved, rejected, blocked) is recorded.
- **A CI-friendly dry-run endpoint** — fail the build *before* a breaking schema is ever submitted.

## How compatibility is decided

The engine ([`CompatibilityChecker`](src/main/java/com/sraavan/dchub/compat/CompatibilityChecker.java)) walks both schema versions — including nested structs — and classifies every difference by the direction it breaks:

| Change | Backward? | Forward? | Why |
|---|---|---|---|
| Add optional field / field with default | safe | safe | nobody is forced to supply or read it |
| Add **required** field (no default) | safe | **breaks** | old writers won't produce it |
| Remove **required** field | **breaks** | safe | new readers still expect it |
| Remove optional field | safe | safe | nothing depended on it |
| Widen type (`INT`→`LONG`) | safe | **breaks** | old data fits new reader, not vice-versa |
| Narrow type (`LONG`→`INT`) | **breaks** | safe | old data may not fit |
| Make field nullable | safe | **breaks** | old readers may not expect null |
| Make field required | **breaks** | safe | old data may contain null |
| struct ↔ scalar | **breaks** | **breaks** | structurally incompatible |

- **BACKWARD** allows a change only if it doesn't break backward (deploy consumers first).
- **FORWARD** allows it only if it doesn't break forward (deploy producers first).
- **FULL** requires both. **NONE** allows anything (but still records the diff).

## Quick start

```bash
# Run Postgres + the service
docker compose up --build

# Or run the service against your own Postgres
DB_URL=jdbc:postgresql://localhost:5432/dchub ./mvnw spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html` · Health: `http://localhost:8080/actuator/health`

## Walkthrough

```bash
# 1. Register a contract governed under BACKWARD compatibility
curl -XPOST localhost:8080/api/contracts -H 'Content-Type: application/json' -d '{
  "name": "orders", "owner": "data-platform", "compatibilityMode": "BACKWARD"
}'

# 2. Submit the first schema version
curl -XPOST localhost:8080/api/contracts/orders/versions -H 'Content-Type: application/json' -d '{
  "submittedBy": "alice",
  "schema": {
    "id":    { "type": "long" },
    "total": { "type": "double" }
  }
}'

# 3. Approve it -> it becomes the current version
curl -XPOST localhost:8080/api/contracts/orders/versions/1/approve \
  -H 'Content-Type: application/json' -d '{ "approver": "bob" }'

# 4. Dry-run a candidate change in CI (no write). Returns compatible:true/false + the diff.
curl -XPOST localhost:8080/api/contracts/orders/check -H 'Content-Type: application/json' -d '{
  "schema": {
    "id":     { "type": "long" },
    "total":  { "type": "int" }
  }
}'
# -> 200 { "compatible": false, "violations": [ { "path": "total", "kind": "TYPE_CHANGED", ... } ] }
```

A breaking submission is rejected with **422 Unprocessable Entity** and the full diff in the `compatibility` field, so a pipeline can print exactly what broke.

## Schema format

A compact JSON object keyed by field name. A field is **required** unless it is `nullable` or declares a `default`.

```json
{
  "id":      { "type": "long" },
  "email":   { "type": "string", "nullable": true },
  "coupon":  { "type": "string", "default": "" },
  "address": { "type": "struct", "fields": {
                 "zip":     { "type": "string" },
                 "country": { "type": "string", "nullable": true }
              }}
}
```

Types: `boolean, int, long, float, double, string, bytes, timestamp, date, struct`.

## API

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/contracts` | Create a contract |
| `GET` | `/api/contracts` | List contracts |
| `GET` | `/api/contracts/{name}` | Get a contract + current version |
| `POST` | `/api/contracts/{name}/check` | Dry-run compatibility (no write) |
| `POST` | `/api/contracts/{name}/versions` | Submit a candidate version |
| `GET` | `/api/contracts/{name}/versions` | List versions |
| `POST` | `/api/contracts/{name}/versions/{v}/approve` | Approve a proposed version |
| `POST` | `/api/contracts/{name}/versions/{v}/reject` | Reject a proposed version |
| `GET` | `/api/contracts/{name}/audit` | Audit trail |

## Architecture

```
web/        REST controllers, RFC-7807 error handling, DTO mapping
service/    ContractService — governance rules + audit, transactional
compat/     pure compatibility engine (no Spring) — Schema, Checker, Parser
domain/     JPA entities: Contract, ContractVersion, AuditEvent
repo/       Spring Data repositories
db/migration/  Flyway schema (Postgres)
```

The compatibility engine is deliberately pure and Spring-free so it is trivial to unit-test and could be reused as a library or CLI. Persistence is Postgres via Flyway in production; tests run on in-memory H2.

## Tech

Java 21 · Spring Boot 3.3 · Spring Data JPA · Flyway · PostgreSQL · springdoc/OpenAPI · JUnit 5 · Docker.

## Roadmap

This is an honest v0.1. Things a production deployment would want next, roughly in order:

- [ ] AuthN/AuthZ (OIDC) and per-contract RBAC for who may approve
- [ ] Avro / Protobuf / JSON-Schema importers so existing schemas drop in
- [ ] Webhooks / events on approval so registries and pipelines can react
- [ ] A CLI + GitHub Action wrapping the `/check` endpoint
- [ ] Soft-delete + deprecation lifecycle for contracts
- [ ] Pagination and search on contracts/audit

Issues and PRs welcome — see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE) © Sraavan Chevireddy
