package com.sraavan.dchub.repo;

import com.sraavan.dchub.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByContractNameOrderByAtDesc(String contractName);
}
