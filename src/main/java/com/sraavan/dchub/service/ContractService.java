package com.sraavan.dchub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sraavan.dchub.compat.CompatibilityChecker;
import com.sraavan.dchub.compat.CompatibilityMode;
import com.sraavan.dchub.compat.CompatibilityResult;
import com.sraavan.dchub.compat.Schema;
import com.sraavan.dchub.compat.SchemaParser;
import com.sraavan.dchub.domain.AuditEvent;
import com.sraavan.dchub.domain.Contract;
import com.sraavan.dchub.domain.ContractVersion;
import com.sraavan.dchub.domain.VersionStatus;
import com.sraavan.dchub.repo.AuditEventRepository;
import com.sraavan.dchub.repo.ContractRepository;
import com.sraavan.dchub.repo.ContractVersionRepository;
import com.sraavan.dchub.service.Dtos.ChangeView;
import com.sraavan.dchub.service.Dtos.CompatibilityView;
import com.sraavan.dchub.service.ServiceExceptions.ConflictException;
import com.sraavan.dchub.service.ServiceExceptions.IncompatibleSchemaException;
import com.sraavan.dchub.service.ServiceExceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Governance core: owns the rules for how contracts evolve.
 *
 * <p>The key invariant: a new version is only persisted if it satisfies the contract's
 * {@link CompatibilityMode} relative to the <em>current approved</em> version. Every
 * state transition is recorded as an {@link AuditEvent}.
 */
@Service
public class ContractService {

    private final ContractRepository contracts;
    private final ContractVersionRepository versions;
    private final AuditEventRepository audit;
    private final SchemaParser parser;
    private final ObjectMapper mapper;
    private final CompatibilityChecker checker = new CompatibilityChecker();

    public ContractService(ContractRepository contracts, ContractVersionRepository versions,
                           AuditEventRepository audit, ObjectMapper mapper) {
        this.contracts = contracts;
        this.versions = versions;
        this.audit = audit;
        this.mapper = mapper;
        this.parser = new SchemaParser(mapper);
    }

    @Transactional
    public Contract createContract(String name, String owner, CompatibilityMode mode) {
        if (contracts.existsByName(name)) {
            throw new ConflictException("Contract '" + name + "' already exists");
        }
        Contract c = contracts.save(new Contract(name, owner,
                mode == null ? CompatibilityMode.BACKWARD : mode));
        audit.save(new AuditEvent(name, "CONTRACT_CREATED", owner,
                "mode=" + c.getCompatibilityMode()));
        return c;
    }

    @Transactional(readOnly = true)
    public Contract getContract(String name) {
        return contracts.findByName(name)
                .orElseThrow(() -> new NotFoundException("No contract named '" + name + "'"));
    }

    @Transactional(readOnly = true)
    public List<Contract> listContracts() {
        return contracts.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ContractVersion> currentApproved(Long contractId) {
        return versions.findFirstByContractIdAndStatusOrderByVersionDesc(
                contractId, VersionStatus.APPROVED);
    }

    /**
     * Run a compatibility check without persisting anything — the endpoint a CI pipeline
     * calls to fail a build before a bad schema is ever submitted.
     */
    @Transactional(readOnly = true)
    public CompatibilityView dryRun(String contractName, String candidateJson) {
        Contract contract = getContract(contractName);
        Schema candidate = parser.parse(candidateJson);
        Optional<ContractVersion> current = currentApproved(contract.getId());

        if (current.isEmpty()) {
            // First version is always compatible (nothing to break).
            return new CompatibilityView(contract.getCompatibilityMode(), true, List.of(), List.of());
        }
        Schema currentSchema = parser.parse(current.get().getSchemaJson());
        CompatibilityResult r = checker.check(currentSchema, candidate, contract.getCompatibilityMode());
        return toView(r);
    }

    /**
     * Submit a candidate version. If it violates the contract's compatibility mode the
     * submission is rejected (422) and an audit event is recorded; otherwise it is stored
     * as a {@code PROPOSED} version awaiting approval.
     */
    @Transactional
    public ContractVersion submitVersion(String contractName, String candidateJson, String submittedBy) {
        Contract contract = getContract(contractName);
        Schema candidate = parser.parse(candidateJson); // validates JSON up front
        Optional<ContractVersion> current = currentApproved(contract.getId());

        if (current.isPresent()) {
            Schema currentSchema = parser.parse(current.get().getSchemaJson());
            CompatibilityResult r = checker.check(
                    currentSchema, candidate, contract.getCompatibilityMode());
            if (!r.compatible()) {
                audit.save(new AuditEvent(contractName, "VERSION_REJECTED_INCOMPATIBLE", submittedBy,
                        r.violations().size() + " violation(s) under " + contract.getCompatibilityMode()));
                throw new IncompatibleSchemaException(
                        "Candidate schema is not " + contract.getCompatibilityMode()
                                + "-compatible with the current version", toView(r));
            }
        }

        int nextVersion = versions.findFirstByContractIdOrderByVersionDesc(contract.getId())
                .map(v -> v.getVersion() + 1).orElse(1);
        ContractVersion saved = versions.save(
                new ContractVersion(contract.getId(), nextVersion, candidateJson, submittedBy));
        audit.save(new AuditEvent(contractName, "VERSION_PROPOSED", submittedBy,
                "version=" + nextVersion));
        return saved;
    }

    @Transactional
    public ContractVersion approve(String contractName, int version, String approver) {
        ContractVersion v = requireVersion(contractName, version);
        if (v.getStatus() != VersionStatus.PROPOSED) {
            throw new ConflictException("Version " + version + " is " + v.getStatus()
                    + ", only PROPOSED versions can be approved");
        }
        v.approve(approver);
        versions.save(v);
        audit.save(new AuditEvent(contractName, "VERSION_APPROVED", approver, "version=" + version));
        return v;
    }

    @Transactional
    public ContractVersion reject(String contractName, int version, String approver) {
        ContractVersion v = requireVersion(contractName, version);
        if (v.getStatus() != VersionStatus.PROPOSED) {
            throw new ConflictException("Version " + version + " is " + v.getStatus()
                    + ", only PROPOSED versions can be rejected");
        }
        v.reject(approver);
        versions.save(v);
        audit.save(new AuditEvent(contractName, "VERSION_REJECTED", approver, "version=" + version));
        return v;
    }

    @Transactional(readOnly = true)
    public List<ContractVersion> listVersions(String contractName) {
        Contract contract = getContract(contractName);
        return versions.findByContractIdOrderByVersionAsc(contract.getId());
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> auditTrail(String contractName) {
        getContract(contractName); // 404 if unknown
        return audit.findByContractNameOrderByAtDesc(contractName);
    }

    private ContractVersion requireVersion(String contractName, int version) {
        Contract contract = getContract(contractName);
        return versions.findByContractIdAndVersion(contract.getId(), version)
                .orElseThrow(() -> new NotFoundException(
                        "Contract '" + contractName + "' has no version " + version));
    }

    private CompatibilityView toView(CompatibilityResult r) {
        return new CompatibilityView(
                r.mode(), r.compatible(),
                r.changes().stream().map(ChangeView::from).toList(),
                r.violations().stream().map(ChangeView::from).toList());
    }
}
