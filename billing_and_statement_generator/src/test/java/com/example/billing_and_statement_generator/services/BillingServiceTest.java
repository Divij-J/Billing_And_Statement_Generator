package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.BillingCycleResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Transaction;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.PaymentRepository;
import com.example.billing_and_statement_generator.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BillingServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private BillingCycleRepository billingCycleRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private CardService cardService;

    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private BillingService billingService;

    private Card card;
    private BillingCycle lastCycle;
    private List<Transaction> unbilledTransactions;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        card = new Card();
        card.setCardId(UUID.randomUUID());
        card.setCreditLimit(new BigDecimal("5000"));
        card.setAnnualInterestRate(new BigDecimal("0.20"));
        card.setCashAdvanceAPR(new BigDecimal("0.24"));
        card.setCashAdvanceBalance(BigDecimal.ZERO);
        card.setLateFeeAmount(new BigDecimal("25.00"));
        card.setAnnualMembershipFee(new BigDecimal("75.00"));
        card.setCardIssueDate(LocalDate.now().minusYears(2));

        lastCycle = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(card)
                .cycleStatus("OPEN")
                .cycleEndDate(LocalDate.now().minusDays(31))
                .totalOutstanding(new BigDecimal("300"))
                .dueDate(LocalDate.now().minusDays(10))
                .build();

        Transaction t1 = Transaction.builder()
                .transactionId(UUID.randomUUID())
                .card(card)
                .transactionType(Transaction.transactionType.PURCHASE)
                .amount(new BigDecimal("100.00"))
                .build();

        unbilledTransactions = List.of(t1);
    }

    // ========================================================================
    // TEST: generateBillingCycle — success
    // ========================================================================

    @Test
    void generateBillingCycle_shouldGenerateSuccessfully() {
        UUID cardId = card.getCardId();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(billingCycleRepository.findTopByCardCardIdOrderByCycleEndDateDesc(cardId))
                .thenReturn(Optional.of(lastCycle));
        when(transactionRepository.findByCardCardIdAndBillingCycleIsNull(cardId))
                .thenReturn(unbilledTransactions);

        when(paymentRepository.findPaymentsWithinCycle(any(), any(), any()))
                .thenReturn(List.of());

        // Stub billing cycle save
        BillingCycle savedCycle = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(card)
                .cycleStartDate(LocalDate.now().minusDays(30))
                .cycleEndDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(25))
                .cycleStatus("OPEN")
                .build();

        when(billingCycleRepository.save(any(BillingCycle.class)))
                .thenReturn(savedCycle);

        // Mock saveAll for transactions
        when(transactionRepository.saveAll(anyList()))
                .thenReturn(unbilledTransactions);

        BillingCycleResponseDTO result =
                billingService.generateBillingCycle(cardId);

        assertNotNull(result);
        assertEquals(savedCycle.getCycleId(), result.getCycleId());
        verify(cardRepository).findById(cardId);
        verify(transactionService, atLeast(0))
                .createInterest(eq(cardId), any(), any());
    }

    // ========================================================================
    // TEST: generateBillingCycle — Card Not Found
    // ========================================================================

    @Test
    void generateBillingCycle_shouldThrowWhenCardNotFound() {
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> billingService.generateBillingCycle(cardId));
    }

    // ========================================================================
    // TEST: getBillingCycle — Success
    // ========================================================================

    @Test
    void getBillingCycle_shouldReturnCycle() {
        UUID cardId = card.getCardId();
        UUID cycleId = UUID.randomUUID();

        BillingCycle cycle = BillingCycle.builder()
                .cycleId(cycleId)
                .card(card)
                .cycleStatus("OPEN")
                .cycleEndDate(LocalDate.now())
                .previousBalance(BigDecimal.ZERO)
                .totalPurchases(BigDecimal.ZERO)
                .totalCashAdvance(BigDecimal.ZERO)
                .totalInterest(BigDecimal.ZERO)
                .totalOutstanding(BigDecimal.ZERO)
                .build();

        when(billingCycleRepository.findById(cycleId))
                .thenReturn(Optional.of(cycle));

        BillingCycleResponseDTO response =
                billingService.getBillingCycle(cardId, cycleId);

        assertNotNull(response);
        assertEquals(cycleId, response.getCycleId());
        verify(billingCycleRepository).findById(cycleId);
    }

    // ========================================================================
    // TEST: getBillingCycle — Wrong Card ID
    // ========================================================================

    @Test
    void getBillingCycle_shouldThrowIfCycleNotBelongToCard() {
        UUID cardId = UUID.randomUUID(); // not matching card.getCardId()
        UUID cycleId = UUID.randomUUID();

        BillingCycle wrongCycle = BillingCycle.builder()
                .cycleId(cycleId)
                .card(card) // different card
                .build();

        when(billingCycleRepository.findById(cycleId))
                .thenReturn(Optional.of(wrongCycle));

        assertThrows(EntityNotFoundException.class,
                () -> billingService.getBillingCycle(cardId, cycleId));
    }

}
