package com.example.billing_and_statement_generator.controller;


import com.example.billing_and_statement_generator.Controller.BillingController;
import com.example.billing_and_statement_generator.dto.BillingCycleResponseDTO;
import com.example.billing_and_statement_generator.dto.card.CardIdDTO;
import com.example.billing_and_statement_generator.dto.transaction.GetTransactionsByCardIdCycleIdRequestDTO;
import com.example.billing_and_statement_generator.services.BillingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BillingController.class)
@AutoConfigureMockMvc(addFilters = false)
class BillingControllerBDDTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillingService billingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGenerateBillingCycle() throws Exception {
        // GIVEN
        UUID cardId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();

        CardIdDTO request = new CardIdDTO();
        request.setCardId(cardId);

        BillingCycleResponseDTO response = new BillingCycleResponseDTO();
        response.setCycleId(cycleId);
        response.setCardId(cardId);

        given(billingService.generateBillingCycle(cardId))
                .willReturn(response);

        // WHEN / THEN
        mockMvc.perform(post("/api/billing/v1/GenerateBillingCycleFeeBreakdown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cycleId").value(cycleId.toString()))
                .andExpect(jsonPath("$.cardId").value(cardId.toString()));

        then(billingService).should().generateBillingCycle(cardId);
    }

    @Test
    void shouldGetBillingCycleByCardAndCycleId() throws Exception {
        // GIVEN
        UUID cardId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();

        GetTransactionsByCardIdCycleIdRequestDTO request =
                new GetTransactionsByCardIdCycleIdRequestDTO();
        request.setCardId(cardId);
        request.setCycleId(cycleId);

        BillingCycleResponseDTO response = new BillingCycleResponseDTO();
        response.setCycleId(cycleId);
        response.setCardId(cardId);

        given(billingService.getBillingCycle(cardId, cycleId))
                .willReturn(response);

        // WHEN / THEN
        mockMvc.perform(post("/api/billing/v1/GetBillingCycleByCardAndCycleId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycleId").value(cycleId.toString()))
                .andExpect(jsonPath("$.cardId").value(cardId.toString()));

        then(billingService).should()
                .getBillingCycle(cardId, cycleId);
    }
}

