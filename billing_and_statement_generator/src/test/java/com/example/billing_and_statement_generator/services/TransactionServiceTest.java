package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionRequestDTO;
import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionResponseDTO;

import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Transaction;
import com.example.billing_and_statement_generator.mapper.TransactionMapper;

import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private CardService cardService;

    @InjectMocks
    private TransactionService transactionService;

    private UUID cardId;
    private Card card;

    private CreateTransactionRequestDTO dto;
    private Transaction txEntity;

    @BeforeEach
    void init() {
        cardId = UUID.randomUUID();

        card = Card.builder()
                .cardId(cardId)
                .cardBalance(BigDecimal.ZERO)
                .cashAdvanceBalance(BigDecimal.ZERO)
                .creditLimit(BigDecimal.valueOf(5000))
                .cashAdvanceFeeRate(BigDecimal.valueOf(0.02))
                .build();

        dto = CreateTransactionRequestDTO.builder()
                .cardId(cardId)
                .transactionType(Transaction.transactionType.PURCHASE)
                .amount(BigDecimal.valueOf(100))
                .transactionDate(LocalDate.now())
                .merchantName("Service Test")
                .build();

        txEntity = Transaction.builder()
                .card(card)
                .amount(dto.getAmount())
                .merchantName(dto.getMerchantName())
                .transactionType(dto.getTransactionType())
                .transactionDate(dto.getTransactionDate())
                .build();
    }

    @Test
    void testCreatePurchaseSuccess() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(transactionMapper.toEntity(dto)).thenReturn(txEntity);

        when(cardService.applyPurchase(cardId, BigDecimal.valueOf(100)))
                .thenReturn(BigDecimal.valueOf(100));

        Transaction savedTx = Transaction.builder()
                .transactionId(UUID.randomUUID())
                .status(Transaction.Status.SENT)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);
        when(transactionMapper.toResponse(savedTx))
                .thenReturn(new CreateTransactionResponseDTO());

        CreateTransactionResponseDTO result = transactionService.create(dto);

        assertNotNull(result, "Expected non-null response when creating a purchase transaction");
        verify(cardService).applyPurchase(cardId, BigDecimal.valueOf(100));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testCreatePurchaseDeclinedLimitExceeded() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(transactionMapper.toEntity(dto)).thenReturn(txEntity);

        doThrow(new CardService.LimitExceededException("limit exceeded"))
                .when(cardService).applyPurchase(cardId, dto.getAmount());

        // When declined, transaction MUST still be saved
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThrows(CardService.LimitExceededException.class,
                () -> transactionService.create(dto),
                "Expected LimitExceededException when purchase exceeds card limit");

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testCreateCashAdvanceSuccess() {
        dto.setTransactionType(Transaction.transactionType.CASHADVANCE);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(transactionMapper.toEntity(dto)).thenReturn(txEntity);

        when(cardService.applyCashAdvance(cardId, dto.getAmount()))
                .thenReturn(BigDecimal.valueOf(200));

        when(cardService.applyFee(eq(cardId), any(BigDecimal.class)))
                .thenReturn(BigDecimal.valueOf(2));

        when(transactionRepository.save(any())).thenReturn(txEntity);
        when(transactionMapper.toResponse(any())).thenReturn(new CreateTransactionResponseDTO());

        CreateTransactionResponseDTO result = transactionService.create(dto);

        assertNotNull(result, "Expected a non-null response after creating a cash advance transaction");

        verify(cardService).applyCashAdvance(cardId, BigDecimal.valueOf(100));

        BigDecimal expectedFee =
                BigDecimal.valueOf(100).multiply(BigDecimal.valueOf(0.02));

        verify(cardService).applyFee(eq(cardId), eq(expectedFee));
    }

    @Test
    void testCreateInterest() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardService.applyInterest(cardId, BigDecimal.valueOf(20),
                TransactionService.InterestType.CARDBALANCE))
                .thenReturn(BigDecimal.valueOf(120));

        transactionService.createInterest(cardId, BigDecimal.valueOf(20),
                TransactionService.InterestType.CARDBALANCE);

        verify(cardService).applyInterest(cardId, BigDecimal.valueOf(20),
                TransactionService.InterestType.CARDBALANCE);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testCreateInterestNullAmount() {
        assertThrows(TransactionService.ValidationException.class,
                () -> transactionService.createInterest(
                        cardId, null,
                        TransactionService.InterestType.CARDBALANCE));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void testCreateInterestZeroAmount() {
        assertThrows(TransactionService.ValidationException.class,
                () -> transactionService.createInterest(
                        cardId, BigDecimal.ZERO,
                        TransactionService.InterestType.CARDBALANCE));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void testCreateFee() {
        LocalDate date = LocalDate.now();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardService.applyFee(cardId, BigDecimal.valueOf(50)))
                .thenReturn(BigDecimal.valueOf(50));

        transactionService.createFee(cardId, BigDecimal.valueOf(50), date, Transaction.transactionType.FEE);

        verify(cardService).applyFee(cardId, BigDecimal.valueOf(50));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testCreateFeeNullAmount() {
        assertThrows(TransactionService.ValidationException.class,
                () -> transactionService.createFee(
                        cardId, null, LocalDate.now(),
                        Transaction.transactionType.LATEFEE));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void testCreateFeeZeroAmount() {
        assertThrows(TransactionService.ValidationException.class,
                () -> transactionService.createFee(
                        cardId, BigDecimal.ZERO, LocalDate.now(),
                        Transaction.transactionType.LATEFEE));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void testGetById() {
        UUID txId = UUID.randomUUID();
        Transaction tx = new Transaction();
        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

        CreateTransactionResponseDTO resp = new CreateTransactionResponseDTO();
        when(transactionMapper.toResponse(tx)).thenReturn(resp);

        CreateTransactionResponseDTO result = transactionService.getById(txId);

        assertNotNull(result, "Expected non-null transaction when retrieving by ID");
    }

    @Test
    void testListByCard() {
        when(transactionRepository.findByCardCardId(cardId))
                .thenReturn(List.of(txEntity));

        when(transactionMapper.toResponse(any()))
                .thenReturn(new CreateTransactionResponseDTO());

        List<CreateTransactionResponseDTO> result = transactionService.listByCard(cardId);

        assertEquals(1, result.size(), "Expected exactly one transaction for the given card");
    }

    @Test
    void testListByCycle() {
        UUID cycleId = UUID.randomUUID();

        when(transactionRepository.findByBillingCycleCycleId(cycleId))
                .thenReturn(List.of(txEntity));

        when(transactionMapper.toResponse(any()))
                .thenReturn(new CreateTransactionResponseDTO());

        var list = transactionService.listByCycle(cycleId);
        assertEquals(1, list.size(), "Expected exactly one transaction for the given billing cycle");
    }

    @Test
    void testListByCardAndDateRange() {
        when(transactionRepository.findByCardCardIdAndTransactionDateBetween(
                eq(cardId),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(List.of(txEntity));

        when(transactionMapper.toResponse(any()))
                .thenReturn(new CreateTransactionResponseDTO());

        var result = transactionService.listByCardAndDateRange(
                cardId, LocalDate.now().minusDays(5), LocalDate.now());

        assertEquals(1, result.size(), "Expected exactly one transaction within the given date range");
    }
}