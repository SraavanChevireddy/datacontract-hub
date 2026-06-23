package com.sraavan.dchub.domain;

/**
 * Lifecycle of a {@link ContractVersion}.
 *
 * <ul>
 *   <li>{@code PROPOSED} — submitted, passed compatibility, awaiting approval.</li>
 *   <li>{@code APPROVED} — accepted by an approver; becomes the current version.</li>
 *   <li>{@code REJECTED} — declined by an approver.</li>
 * </ul>
 */
public enum VersionStatus {
    PROPOSED,
    APPROVED,
    REJECTED
}
