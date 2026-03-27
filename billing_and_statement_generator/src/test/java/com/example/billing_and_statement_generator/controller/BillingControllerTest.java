package com.example.billing_and_statement_generator.controller;

import com.example.billing_and_statement_generator.Controller.BillingController;
import com.example.billing_and_statement_generator.dto.BillingCycleResponseDTO;
import com.example.billing_and_statement_generator.dto.v1.BillingCycleResponseV1DTO;
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
    // POST /api/billing/generate/{cardId}
    // ------------------------------------------------------------
    @WithMockUser
    @Test
    void generateBillingCycle_shouldReturn201() throws Exception {
        UUID cardId = UUID.randomUUID();

        BillingCycleResponseDTO response = new BillingCycleResponseDTO();
        response.setCycleId(UUID.randomUUID());
        response.setCardId(cardId);
        response.setCycleStatus("OPEN");

        Mockito.when(billingService.generateBillingCycle(cardId))
                .thenReturn(response);

        mockMvc.perform(post("/api/billing/generate/" + cardId)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cycleId").value(response.getCycleId().toString()))
                .andExpect(jsonPath("$.cardId").value(cardId.toString()));
    }

    // ------------------------------------------------------------
    // POST /api/billing/v1/generate/{cardId}
    // ------------------------------------------------------------
    @WithMockUser
    @Test
    void generateBillingCycleV1_shouldReturn201() throws Exception {
        UUID cardId = UUID.randomUUID();

        BillingCycleResponseV1DTO response = new BillingCycleResponseV1DTO();
        response.setCycleId(UUID.randomUUID());
        response.setCardId(cardId);
        response.setCycleStatus("OPEN");

        Mockito.when(billingService.generateBillingCycleV1(cardId))
                .thenReturn(response);

        mockMvc.perform(post("/api/billing/v1/generate/" + cardId)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cycleId").value(response.getCycleId().toString()))
                .andExpect(jsonPath("$.cardId").value(cardId.toString()));
    }

    // ------------------------------------------------------------
    // POST /api/billing/{cardId}/{cycleId}
    // ------------------------------------------------------------
    @WithMockUser
    @Test
    void getBillingCycle_shouldReturn200() throws Exception {
        UUID cardId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();

        BillingCycleResponseDTO response = new BillingCycleResponseDTO();
        response.setCycleId(cycleId);
        response.setCardId(cardId);
        response.setCycleStatus("OPEN");

        Mockito.when(billingService.getBillingCycle(cardId, cycleId))
                .thenReturn(response);

        mockMvc.perform(post("/api/billing/" + cardId + "/" + cycleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycleId").value(cycleId.toString()))
                .andExpect(jsonPath("$.cardId").value(cardId.toString()))
                .andExpect(jsonPath("$.cycleStatus").value("OPEN"));
    }

    // ------------------------------------------------------------
    // POST /api/billing/v1/{cardId}/{cycleId}
    // ------------------------------------------------------------
    @WithMockUser
    @Test
    void getBillingCycleV1_shouldReturn200() throws Exception {
        UUID cardId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();

        BillingCycleResponseV1DTO response = new BillingCycleResponseV1DTO();
        response.setCycleId(cycleId);
        response.setCardId(cardId);
        response.setCycleStatus("OPEN");

        Mockito.when(billingService.getBillingCycleV1(cardId, cycleId))
                .thenReturn(response);

        mockMvc.perform(post("/api/billing/v1/" + cardId + "/" + cycleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycleId").value(cycleId.toString()))
                .andExpect(jsonPath("$.cardId").value(cardId.toString()))
                .andExpect(jsonPath("$.cycleStatus").value("OPEN"));
    }
}