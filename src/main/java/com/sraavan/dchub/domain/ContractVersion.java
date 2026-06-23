package com.sraavan.dchub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * One immutable version of a {@link Contract}'s schema. The schema document is stored
 * verbatim (the JSON the submitter sent) so the contract history is auditable and the
 * compatibility engine can re-parse it on demand.
 */
@Entity
@Table(name = "contract_version",
        uniqueConstraints = @UniqueConstraint(columnNames = {"contract_id", "version"}),
        indexes = @Index(name = "idx_cv_contract", columnList = "contract_id"))
public class ContractVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(nullable = false)
    private int version;

    @Lob
    @Column(name = "schema_json", nullable = false)
    private String schemaJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VersionStatus status = VersionStatus.PROPOSED;

    @Column(nullable = false)
    private String submittedBy;

    @Column(nullable = false)
    private Instant submittedAt = Instant.now();

    private String decidedBy;

    private Instant decidedAt;

    protected ContractVersion() {
    }

    public ContractVersion(Long contractId, int version, String schemaJson, String submittedBy) {
        this.contractId = contractId;
        this.version = version;
        this.schemaJson = schemaJson;
        this.submittedBy = submittedBy;
        this.status = VersionStatus.PROPOSED;
        this.submittedAt = Instant.now();
    }

    public void approve(String approver) {
        this.status = VersionStatus.APPROVED;
        this.decidedBy = approver;
        this.decidedAt = Instant.now();
    }

    public void reject(String approver) {
        this.status = VersionStatus.REJECTED;
        this.decidedBy = approver;
        this.decidedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getContractId() {
        return contractId;
    }

    public int getVersion() {
        return version;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public VersionStatus getStatus() {
        return status;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
