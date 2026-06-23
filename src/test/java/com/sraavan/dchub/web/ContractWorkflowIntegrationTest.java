package com.sraavan.dchub.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContractWorkflowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private void createContract(String name, String mode) throws Exception {
        mvc.perform(post("/api/contracts").contentType(MediaType.APPLICATION_JSON).content("""
                        { "name": "%s", "owner": "data-platform", "compatibilityMode": "%s" }
                        """.formatted(name, mode)))
                .andExpect(status().isCreated());
    }

    @Test
    void fullGovernanceWorkflow() throws Exception {
        createContract("orders", "BACKWARD");

        // v1 — first version, always accepted, then approve it.
        mvc.perform(post("/api/contracts/orders/versions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "submittedBy": "alice", "schema": {
                                    "id":    { "type": "long" },
                                    "total": { "type": "double" }
                                }}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(jsonPath("$.status", is("PROPOSED")));

        mvc.perform(post("/api/contracts/orders/versions/1/approve")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"approver\": \"bob\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        // A backward-compatible change (add optional field) is accepted.
        mvc.perform(post("/api/contracts/orders/versions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "submittedBy": "alice", "schema": {
                                    "id":     { "type": "long" },
                                    "total":  { "type": "double" },
                                    "coupon": { "type": "string", "nullable": true }
                                }}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version", is(2)));
    }

    @Test
    void incompatibleChangeIsRejectedWith422() throws Exception {
        createContract("payments", "BACKWARD");

        mvc.perform(post("/api/contracts/payments/versions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "submittedBy": "alice", "schema": {
                                    "id":     { "type": "long" },
                                    "amount": { "type": "long" }
                                }}"""))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/contracts/payments/versions/1/approve")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"approver\": \"bob\" }"))
                .andExpect(status().isOk());

        // Narrowing amount LONG -> INT breaks backward compatibility.
        mvc.perform(post("/api/contracts/payments/versions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "submittedBy": "alice", "schema": {
                                    "id":     { "type": "long" },
                                    "amount": { "type": "int" }
                                }}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.compatibility.compatible", is(false)));
    }

    @Test
    void dryRunCheckDoesNotPersist() throws Exception {
        createContract("events", "FULL");
        mvc.perform(post("/api/contracts/events/versions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "submittedBy": "alice", "schema": {
                                    "id": { "type": "long" }
                                }}"""))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/contracts/events/versions/1/approve")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"approver\": \"bob\" }"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/contracts/events/check").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "schema": {
                                    "id":   { "type": "long" },
                                    "extra":{ "type": "string", "nullable": true }
                                }}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible", is(true)));

        // Still only one version persisted.
        mvc.perform(get("/api/contracts/events/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void auditTrailRecordsActions() throws Exception {
        createContract("inventory", "BACKWARD");
        mvc.perform(post("/api/contracts/inventory/versions").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"submittedBy\": \"alice\", \"schema\": { \"id\": { \"type\": \"long\" } } }"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/contracts/inventory/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2))); // CONTRACT_CREATED + VERSION_PROPOSED
    }

    @Test
    void duplicateContractReturns409() throws Exception {
        createContract("dupe", "BACKWARD");
        mvc.perform(post("/api/contracts").contentType(MediaType.APPLICATION_JSON).content("""
                        { "name": "dupe", "owner": "x", "compatibilityMode": "BACKWARD" }"""))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownContractReturns404() throws Exception {
        mvc.perform(get("/api/contracts/does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
