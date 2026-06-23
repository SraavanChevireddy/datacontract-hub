package com.sraavan.dchub.repo;

import com.sraavan.dchub.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByName(String name);

    boolean existsByName(String name);
}
