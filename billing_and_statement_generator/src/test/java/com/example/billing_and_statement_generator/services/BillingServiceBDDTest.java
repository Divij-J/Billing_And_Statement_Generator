package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.BillingCycleResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Transaction;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.PaymentRepository;
import com.example.billing_and_statement_generator.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class BillingServiceBDDTest {

    @Mock
    CardRepository cardRepository;

    @Mock
    BillingCycleRepository billingCycleRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    TransactionService transactionService;

    @Mock
    CardService cardService;

    @InjectMocks
    BillingService billingService;

    private UUID cardId;
    private Card card;

    @BeforeEach
    void setup() {
        cardId = UUID.randomUUID();

        card = Card.builder()
                .cardId(cardId)
                .creditLimit(BigDecimal.valueOf(5000))
                .cardBalance(BigDecimal.ZERO)
                .cashAdvanceBalance(BigDecimal.ZERO)
                .annualInterestRate(new BigDecimal("0.20"))
                .active(true)
                .build();
    }

    @Test
    void shouldGenerateBillingCycleForFirstTimeCard() {
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        given(billingCycleRepository
                .findTopByCardCardIdOrderByCycleEndDateDesc(cardId))
                .willReturn(Optional.empty());

        given(transactionRepository
                .findByCardCardIdAndBillingCycleIsNull(cardId))
                .willReturn(List.of());

        given(paymentRepository.findPaymentsWithinCycle(
                any(), any(), any()))
                .willReturn(List.of());

        given(billingCycleRepository.save(any(BillingCycle.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        BillingCycleResponseDTO response =
                billingService.generateBillingCycle(cardId);

        // THEN
        assertEquals(cardId, response.getCardId());
        assertEquals("OPEN", response.getCycleStatus());
        assertEquals(BigDecimal.ZERO, response.getTotalInterest());

        then(billingCycleRepository).should().save(any(BillingCycle.class));
        then(transactionService).should(never())
                .createInterest(any(), any(), any());
    }

    @Test
    void shouldFailWhenCardDoesNotExist() {
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(EntityNotFoundException.class,
                () -> billingService.generateBillingCycle(cardId),
                "Expected failure when card does not exist");

        then(billingCycleRepository).shouldHaveNoInteractions();
        then(transactionRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldClosePreviousOpenBillingCycle() {
        // GIVEN
        BillingCycle previousCycle = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(card)
                .cycleStatus("OPEN")
                .cycleStartDate(LocalDate.now().minusDays(30))
                .cycleEndDate(LocalDate.now().minusDays(1))
                .dueDate(LocalDate.now().minusDays(5))
                .totalOutstanding(BigDecimal.valueOf(500))
                .build();

        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        given(billingCycleRepository
                .findTopByCardCardIdOrderByCycleEndDateDesc(cardId))
                .willReturn(Optional.of(previousCycle));

        given(transactionRepository
                .findByCardCardIdAndBillingCycleIsNull(cardId))
                .willReturn(List.of());

        given(paymentRepository.findPaymentsWithinCycle(
                any(), any(), any()))
                .willReturn(List.of());

        given(billingCycleRepository.save(any(BillingCycle.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        billingService.generateBillingCycle(cardId);

        // THEN
        assertEquals("CLOSED", previousCycle.getCycleStatus());
        then(billingCycleRepository).should().save(previousCycle);
    }

    @Test
    void shouldAttachUnbilledTransactionsToNewCycle() {
        // GIVEN
        Transaction txn;
        txn = Transaction.builder()
                .transactionId(UUID.randomUUID())
                .card(card)
                .amount(BigDecimal.valueOf(100))
                .transactionType(Transaction.transactionType.PURCHASE)
                .build();

        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        given(billingCycleRepository
                .findTopByCardCardIdOrderByCycleEndDateDesc(cardId))
                .willReturn(Optional.empty());

        given(transactionRepository
                .findByCardCardIdAndBillingCycleIsNull(cardId))
                .willReturn(List.of(txn));

        given(paymentRepository.findPaymentsWithinCycle(
                any(), any(), any()))
                .willReturn(List.of());

        given(billingCycleRepository.save(any(BillingCycle.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        billingService.generateBillingCycle(cardId);

        // THEN
        assertNotNull(txn.getBillingCycle());
        then(transactionRepository).should(atLeastOnce()).saveAll(anyList());
    }
}