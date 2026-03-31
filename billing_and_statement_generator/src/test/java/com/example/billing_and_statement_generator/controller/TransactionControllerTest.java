package com.example.billing_and_statement_generator.controller;

import com.example.billing_and_statement_generator.Controller.TransactionController;
import com.example.billing_and_statement_generator.dto.BillingCycleIdDTO;
import com.example.billing_and_statement_generator.dto.card.CardIdDTO;
import com.example.billing_and_statement_generator.dto.transaction.*;
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

        mockMvc.perform(post("/api/transactions/v1/createTransaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.cardId").value(dto.getCardId().toString()));
    }

    @Test
    void testGetTransactionById() throws Exception {
        UUID txId = UUID.randomUUID();

        TransactionIdDTO request = new TransactionIdDTO();
        request.setTransactionId(txId);

        CreateTransactionResponseDTO response = CreateTransactionResponseDTO.builder()
                .transactionId(txId)
                .cardId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(50))
                .build();

        when(transactionService.getById(txId)).thenReturn(response);

        mockMvc.perform(post("/api/transactions/v1/getTransactionById")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(txId.toString()));
    }

    @Test
    void testListByCard() throws Exception {
        UUID cardId = UUID.randomUUID();

        CardIdDTO request = new CardIdDTO();
        request.setCardId(cardId);

        List<CreateTransactionResponseDTO> list = List.of(
                CreateTransactionResponseDTO.builder().transactionId(UUID.randomUUID()).cardId(cardId).build(),
                CreateTransactionResponseDTO.builder().transactionId(UUID.randomUUID()).cardId(cardId).build()
        );

        when(transactionService.listByCard(cardId)).thenReturn(list);

        mockMvc.perform(post("/api/transactions/v1/getTransactionsByCardId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testListByCycle() throws Exception {
        UUID cycleId = UUID.randomUUID();

        BillingCycleIdDTO request = new BillingCycleIdDTO();
        request.setCycleId(cycleId);

        List<CreateTransactionResponseDTO> list = List.of(
                CreateTransactionResponseDTO.builder().transactionId(UUID.randomUUID()).build()
        );

        when(transactionService.listByCycle(cycleId)).thenReturn(list);

        mockMvc.perform(post("/api/transactions/v1/getTransactionsByBillingCycle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testListByDateRange() throws Exception {
        UUID cardId = UUID.randomUUID();

        GetTransactionsBetweenDatesRequestDTO request = new GetTransactionsBetweenDatesRequestDTO();
        request.setCardId(cardId);
        request.setStartDate(LocalDate.of(2024, 1, 1));
        request.setEndDate(LocalDate.of(2024, 1, 31));

        List<CreateTransactionResponseDTO> list = List.of(
                CreateTransactionResponseDTO.builder().transactionId(UUID.randomUUID()).cardId(cardId).build()
        );

        when(transactionService.listByCardAndDateRange(eq(cardId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(list);

        mockMvc.perform(post("/api/transactions/v1/getTransactionsWithinDateRange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testListByCardAndCycle() throws Exception {
        UUID cardId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();

        GetTransactionsByCardIdCycleIdRequestDTO request =
                new GetTransactionsByCardIdCycleIdRequestDTO(cardId, cycleId);

        List<CreateTransactionResponseDTO> list = List.of(
                CreateTransactionResponseDTO.builder().transactionId(UUID.randomUUID()).cardId(cardId).build()
        );

        when(transactionService.listByCardAndBillingCycle(cardId, cycleId))
                .thenReturn(list);

        mockMvc.perform(post("/api/transactions/v1/getTransactionsByCardAndCycleID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}