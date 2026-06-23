package com.sraavan.dchub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An append-only audit record. Every governance-significant action (contract created,
 * version proposed, approved, rejected, compatibility rejected) writes one row here so
 * the history is tamper-evident and queryable.
 */
@Entity
@Table(name = "audit_event",
        indexes = @Index(name = "idx_audit_contract", columnList = "contract_name"))
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_name", nullable = false)
    private String contractName;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String actor;

    @Lob
    @Column(name = "detail")
    private String detail;

    @Column(nullable = false)
    private Instant at = Instant.now();

    protected AuditEvent() {
    }

    public AuditEvent(String contractName, String action, String actor, String detail) {
        this.contractName = contractName;
        this.action = action;
        this.actor = actor;
        this.detail = detail;
        this.at = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getContractName() {
        return contractName;
    }

    public String getAction() {
        return action;
    }

    public String getActor() {
        return actor;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getAt() {
        return at;
    }
}
