package com.example.billing_and_statement_generator.controller;

import com.example.billing_and_statement_generator.Controller.CardController;
import com.example.billing_and_statement_generator.dto.CreateCardRequestDTO;
import com.example.billing_and_statement_generator.dto.CreateCardResponseDTO;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.services.CardService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)
class CardControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    CardService cardService;

    @Test
    void testCreateCard() throws Exception {
        CreateCardRequestDTO dto = CreateCardRequestDTO.builder()
                .customerId(UUID.randomUUID())
                .cardNumber("4111111111111111")
                .cardType(Card.CardType.CREDIT)
                .cardHolderName("John Doe")
                .securityCode("123")
                .build();

        CreateCardResponseDTO responseDTO = CreateCardResponseDTO.builder()
                .cardId(UUID.randomUUID())
                .customerId(dto.getCustomerId())
                .build();

        Mockito.when(cardService.create(any(CreateCardRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/cards/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardId").exists())
                .andExpect(jsonPath("$.customerId").value(dto.getCustomerId().toString()));
    }

    @Test
    void testGetCard() throws Exception {
        UUID cardId = UUID.randomUUID();
        CreateCardResponseDTO responseDTO = CreateCardResponseDTO.builder()
                .cardId(cardId)
                .customerId(UUID.randomUUID())
                .build();

        Mockito.when(cardService.getById(cardId)).thenReturn(responseDTO);

        mockMvc.perform(post("/api/cards/v1/" + cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(cardId.toString()));
    }

    @Test
    void testGetCardsByCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();

        CreateCardResponseDTO card1 = CreateCardResponseDTO.builder().cardId(UUID.randomUUID()).customerId(customerId).build();
        CreateCardResponseDTO card2 = CreateCardResponseDTO.builder().cardId(UUID.randomUUID()).customerId(customerId).build();

        Mockito.when(cardService.getByCustomer(customerId))
                .thenReturn(List.of(card1, card2));

        mockMvc.perform(post("/api/cards/v1/customer/cards/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetBalance() throws Exception {
        UUID cardId = UUID.randomUUID();

        Mockito.when(cardService.getTotalBalance(cardId))
                .thenReturn(BigDecimal.valueOf(250.00));

        mockMvc.perform(post("/api/cards/v1/balance/" + cardId))
                .andExpect(status().isOk())
                .andExpect(content().string("250.0"));
    }
}