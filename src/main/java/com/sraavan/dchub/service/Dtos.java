package com.sraavan.dchub.service;

import com.sraavan.dchub.compat.Change;
import com.sraavan.dchub.compat.CompatibilityMode;
import com.sraavan.dchub.domain.VersionStatus;

import java.time.Instant;
import java.util.List;

/** Request/response payloads for the REST API. Kept together for brevity. */
public final class Dtos {

    private Dtos() {
    }

    public record CreateContractRequest(String name, String owner, CompatibilityMode compatibilityMode) {
    }

    public record ContractResponse(
            Long id, String name, String owner, CompatibilityMode compatibilityMode,
            Integer currentVersion, Instant createdAt) {
    }

    /** Submit a new candidate version. {@code schema} is the raw schema JSON document. */
    public record SubmitVersionRequest(String submittedBy, Object schema) {
    }

    public record ChangeView(String path, String kind, boolean breaksBackward,
                             boolean breaksForward, String detail) {
        public static ChangeView from(Change c) {
            return new ChangeView(c.path(), c.kind().name(), c.breaksBackward(),
                    c.breaksForward(), c.detail());
        }
    }

    /** Returned by both the dry-run check endpoint and a successful submission. */
    public record CompatibilityView(
            CompatibilityMode mode, boolean compatible,
            List<ChangeView> changes, List<ChangeView> violations) {
    }

    public record VersionResponse(
            Long id, String contractName, int version, VersionStatus status,
            String submittedBy, Instant submittedAt, String decidedBy, Instant decidedAt) {
    }

    public record DecisionRequest(String approver) {
    }

    public record AuditView(String contractName, String action, String actor,
                            String detail, Instant at) {
    }
}
