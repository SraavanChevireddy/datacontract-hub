package com.sraavan.dchub.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sraavan.dchub.domain.Contract;
import com.sraavan.dchub.domain.ContractVersion;
import com.sraavan.dchub.domain.VersionStatus;
import com.sraavan.dchub.service.ContractService;
import com.sraavan.dchub.service.Dtos.AuditView;
import com.sraavan.dchub.service.Dtos.CompatibilityView;
import com.sraavan.dchub.service.Dtos.ContractResponse;
import com.sraavan.dchub.service.Dtos.CreateContractRequest;
import com.sraavan.dchub.service.Dtos.DecisionRequest;
import com.sraavan.dchub.service.Dtos.SubmitVersionRequest;
import com.sraavan.dchub.service.Dtos.VersionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for the data-contract governance service.
 *
 * <pre>
 * POST   /api/contracts                          create a contract
 * GET    /api/contracts                          list contracts
 * GET    /api/contracts/{name}                   get one contract
 * POST   /api/contracts/{name}/check             dry-run compatibility (no write)
 * POST   /api/contracts/{name}/versions          submit a candidate version
 * GET    /api/contracts/{name}/versions          list versions
 * POST   /api/contracts/{name}/versions/{v}/approve
 * POST   /api/contracts/{name}/versions/{v}/reject
 * GET    /api/contracts/{name}/audit             audit trail
 * </pre>
 */
@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService service;
    private final ObjectMapper mapper;

    public ContractController(ContractService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ContractResponse> create(@RequestBody CreateContractRequest req) {
        Contract c = service.createContract(req.name(), req.owner(), req.compatibilityMode());
        return ResponseEntity.status(HttpStatus.CREATED).body(Mappers.contract(c, null));
    }

    @GetMapping
    public List<ContractResponse> list() {
        return service.listContracts().stream()
                .map(c -> Mappers.contract(c,
                        service.currentApproved(c.getId()).map(ContractVersion::getVersion).orElse(null)))
                .toList();
    }

    @GetMapping("/{name}")
    public ContractResponse get(@PathVariable String name) {
        Contract c = service.getContract(name);
        Integer cur = service.currentApproved(c.getId()).map(ContractVersion::getVersion).orElse(null);
        return Mappers.contract(c, cur);
    }

    @PostMapping("/{name}/check")
    public CompatibilityView check(@PathVariable String name, @RequestBody SubmitVersionRequest req) {
        return service.dryRun(name, toJson(req.schema()));
    }

    @PostMapping("/{name}/versions")
    public ResponseEntity<VersionResponse> submit(@PathVariable String name,
                                                  @RequestBody SubmitVersionRequest req) {
        ContractVersion v = service.submitVersion(name, toJson(req.schema()), req.submittedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(Mappers.version(v, name));
    }

    @GetMapping("/{name}/versions")
    public List<VersionResponse> versions(@PathVariable String name) {
        return service.listVersions(name).stream().map(v -> Mappers.version(v, name)).toList();
    }

    @PostMapping("/{name}/versions/{version}/approve")
    public VersionResponse approve(@PathVariable String name, @PathVariable int version,
                                   @RequestBody DecisionRequest req) {
        return Mappers.version(service.approve(name, version, req.approver()), name);
    }

    @PostMapping("/{name}/versions/{version}/reject")
    public VersionResponse reject(@PathVariable String name, @PathVariable int version,
                                  @RequestBody DecisionRequest req) {
        return Mappers.version(service.reject(name, version, req.approver()), name);
    }

    @GetMapping("/{name}/audit")
    public List<AuditView> audit(@PathVariable String name) {
        return service.auditTrail(name).stream().map(Mappers::audit).toList();
    }

    private String toJson(Object schema) {
        try {
            return mapper.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid schema payload: " + e.getMessage());
        }
    }
}
