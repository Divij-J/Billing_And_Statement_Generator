package com.example.billing_and_statement_generator.controller;

import com.example.billing_and_statement_generator.Controller.TransactionController;
import com.example.billing_and_statement_generator.dto.CreateTransactionRequestDTO;
import com.example.billing_and_statement_generator.dto.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.entity.Transaction;
import com.example.billing_and_statement_generator.services.TransactionService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    TransactionService transactionService;

    @Test
    void testCreateTransaction() throws Exception {

        CreateTransactionRequestDTO dto = CreateTransactionRequestDTO.builder()
                .cardId(UUID.randomUUID())
                .transactionType(Transaction.transactionType.PURCHASE)
                .amount(BigDecimal.valueOf(100))
                .merchantName("Amazon")
                .transactionDate(LocalDate.now())
                .build();

        CreateTransactionResponseDTO response = CreateTransactionResponseDTO.builder()
                .transactionId(UUID.randomUUID())
                .cardId(dto.getCardId())
                .amount(dto.getAmount())
                .build();

        when(transactionService.create(any(CreateTransactionRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/transactions/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.cardId").value(dto.getCardId().toString()));
    }

    @Test
    void testGetById() throws Exception {
        UUID txId = UUID.randomUUID();

        CreateTransactionResponseDTO response = CreateTransactionResponseDTO.builder()
                .transactionId(txId)
                .cardId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(50))
                .build();

        when(transactionService.getById(txId)).thenReturn(response);

        mockMvc.perform(post("/api/transactions/v1/" + txId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(txId.toString()));
    }

    @Test
    void testListByCard() throws Exception {
        UUID cardId = UUID.randomUUID();

        List<CreateTransactionResponseDTO> list = List.of(
                CreateTransactionResponseDTO.builder().transactionId(UUID.randomUUID()).cardId(cardId).build(),
                CreateTransactionResponseDTO.builder().transactionId(UUID.randomUUID()).cardId(cardId).build()
        );

        when(transactionService.listByCard(cardId)).thenReturn(list);

        mockMvc.perform(post("/api/transactions/v1/card/" + cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testListByCycle() throws Exception {
        UUID cycleId = UUID.randomUUID();

        List<CreateTransactionResponseDTO> list = List.of(
                CreateTransactionResponseDTO.builder().transactionId(UUID.randomUUID()).build()
        );

        when(transactionService.listByCycle(cycleId)).thenReturn(list);

        mockMvc.perform(post("/api/transactions/v1/billing-cycle/" + cycleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testListByDateRange() throws Exception {
        UUID cardId = UUID.randomUUID();

        List<CreateTransactionResponseDTO> list = List.of(
                CreateTransactionResponseDTO.builder()
                        .transactionId(UUID.randomUUID())
                        .cardId(cardId)
                        .build()
        );

        when(transactionService.listByCardAndDateRange(
                eq(cardId),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(list);

        mockMvc.perform(post("/api/transactions/v1/card/range/" + cardId)
                        .param("start", "2024-01-01")
                        .param("end", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testListByCardAndCycle() throws Exception {
        UUID cardId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();

        List<CreateTransactionResponseDTO> list = List.of(
                CreateTransactionResponseDTO.builder()
                        .transactionId(UUID.randomUUID())
                        .cardId(cardId)
                        .build()
        );

        when(transactionService.listByCardAndBillingCycle(cardId, cycleId))
                .thenReturn(list);

        mockMvc.perform(post("/api/transactions/v1/card/billing-cycle/" + cardId + "/" + cycleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}