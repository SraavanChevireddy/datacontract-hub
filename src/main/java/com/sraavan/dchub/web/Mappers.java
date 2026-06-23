package com.sraavan.dchub.web;

import com.sraavan.dchub.domain.AuditEvent;
import com.sraavan.dchub.domain.Contract;
import com.sraavan.dchub.domain.ContractVersion;
import com.sraavan.dchub.service.Dtos.AuditView;
import com.sraavan.dchub.service.Dtos.ContractResponse;
import com.sraavan.dchub.service.Dtos.VersionResponse;

/** Maps domain entities to API response records. */
final class Mappers {

    private Mappers() {
    }

    static ContractResponse contract(Contract c, Integer currentVersion) {
        return new ContractResponse(c.getId(), c.getName(), c.getOwner(),
                c.getCompatibilityMode(), currentVersion, c.getCreatedAt());
    }

    static VersionResponse version(ContractVersion v, String contractName) {
        return new VersionResponse(v.getId(), contractName, v.getVersion(), v.getStatus(),
                v.getSubmittedBy(), v.getSubmittedAt(), v.getDecidedBy(), v.getDecidedAt());
    }

    static AuditView audit(AuditEvent e) {
        return new AuditView(e.getContractName(), e.getAction(), e.getActor(),
                e.getDetail(), e.getAt());
    }
}
