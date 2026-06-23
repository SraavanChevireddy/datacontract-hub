package com.sraavan.dchub.repo;

import com.sraavan.dchub.domain.ContractVersion;
import com.sraavan.dchub.domain.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractVersionRepository extends JpaRepository<ContractVersion, Long> {

    List<ContractVersion> findByContractIdOrderByVersionAsc(Long contractId);

    Optional<ContractVersion> findFirstByContractIdAndStatusOrderByVersionDesc(
            Long contractId, VersionStatus status);

    Optional<ContractVersion> findFirstByContractIdOrderByVersionDesc(Long contractId);

    Optional<ContractVersion> findByContractIdAndVersion(Long contractId, int version);
}
