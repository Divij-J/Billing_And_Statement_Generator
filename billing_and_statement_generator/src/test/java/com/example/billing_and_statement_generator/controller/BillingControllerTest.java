package com.example.billing_and_statement_generator.controller;

import com.example.billing_and_statement_generator.Controller.BillingController;
import com.example.billing_and_statement_generator.dto.BillingCycleResponseDTO;
import com.example.billing_and_statement_generator.services.BillingService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BillingController.class)
class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillingService billingService;

    @Autowired
    private ObjectMapper objectMapper;

    // ------------------------------------------------------------
    // POST /api/billing/v1/GenerateBillingCycleFeeBreakdown
    // ------------------------------------------------------------
    @WithMockUser
    @Test
    void generateBillingCycleV1_shouldReturn201() throws Exception {
        UUID cardId = UUID.randomUUID();

        BillingCycleResponseDTO response = new BillingCycleResponseDTO();
        response.setCycleId(UUID.randomUUID());
        response.setCardId(cardId);
        response.setCycleStatus("OPEN");
        Mockito.when(billingService.generateBillingCycle(cardId)).thenReturn(response);

        String body = "{ \"cardId\": \"" + cardId + "\" }";

        mockMvc.perform(post("/api/billing/v1/GenerateBillingCycleFeeBreakdown")
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cycleId").value(response.getCycleId().toString()))
                .andExpect(jsonPath("$.cardId").value(cardId.toString()));
    }

    // ------------------------------------------------------------
    // POST /api/billing/v1/GetBillingCycleByCardAndCycleId
    // ------------------------------------------------------------
    @WithMockUser
    @Test
    void getBillingCycleV1_shouldReturn200() throws Exception {
        UUID cardId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();

        BillingCycleResponseDTO response = new BillingCycleResponseDTO();
        response.setCycleId(cycleId);
        response.setCardId(cardId);
        response.setCycleStatus("OPEN");

        Mockito.when(billingService.getBillingCycle(cardId, cycleId)).thenReturn(response);

        String body = "{ \"cardId\": \"" + cardId + "\", \"cycleId\": \"" + cycleId + "\" }";

        mockMvc.perform(post("/api/billing/v1/GetBillingCycleByCardAndCycleId")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycleId").value(cycleId.toString()))
                .andExpect(jsonPath("$.cardId").value(cardId.toString()))
                .andExpect(jsonPath("$.cycleStatus").value("OPEN"));
    }
}