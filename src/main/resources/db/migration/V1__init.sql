-- Initial schema for datacontract-hub.

CREATE TABLE contract (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(255) NOT NULL UNIQUE,
    owner              VARCHAR(255) NOT NULL,
    compatibility_mode VARCHAR(32)  NOT NULL,
    created_at         TIMESTAMP    NOT NULL
);

CREATE TABLE contract_version (
    id           BIGSERIAL PRIMARY KEY,
    contract_id  BIGINT       NOT NULL REFERENCES contract (id),
    version      INT          NOT NULL,
    schema_json  TEXT         NOT NULL,
    status       VARCHAR(32)  NOT NULL,
    submitted_by VARCHAR(255) NOT NULL,
    submitted_at TIMESTAMP    NOT NULL,
    decided_by   VARCHAR(255),
    decided_at   TIMESTAMP,
    CONSTRAINT uq_contract_version UNIQUE (contract_id, version)
);

CREATE INDEX idx_cv_contract ON contract_version (contract_id);

CREATE TABLE audit_event (
    id            BIGSERIAL PRIMARY KEY,
    contract_name VARCHAR(255) NOT NULL,
    action        VARCHAR(64)  NOT NULL,
    actor         VARCHAR(255) NOT NULL,
    detail        TEXT,
    at            TIMESTAMP    NOT NULL
);

CREATE INDEX idx_audit_contract ON audit_event (contract_name);
