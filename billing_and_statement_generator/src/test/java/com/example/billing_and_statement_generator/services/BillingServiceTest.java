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

    @Mock private CardRepository cardRepository;
    @Mock private BillingCycleRepository billingCycleRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionService transactionService;
    @Mock private CardService cardService;
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
        card.setCardBalance(BigDecimal.ZERO);
        card.setCashAdvanceBalance(BigDecimal.ZERO);
        card.setAvailableCredit(new BigDecimal("5000"));
        card.setLateFeeAmount(new BigDecimal("25.00"));
        card.setAnnualMembershipFee(BigDecimal.ZERO); // no membership fee by default
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
                .merchantName("TEST")
                .transactionDate(LocalDate.now())
                .status(Transaction.Status.SENT)
                .build();

        unbilledTransactions = List.of(t1);
    }

// ── generateBillingCycle — success ─────────────────────────────────────

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

        BillingCycle savedCycle = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(card)
                .cycleStartDate(LocalDate.now().minusDays(30))
                .cycleEndDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .totalOutstanding(BigDecimal.ZERO)
                .minimumDue(BigDecimal.ZERO)
                .totalPurchases(new BigDecimal("100.00"))
                .totalCashAdvance(BigDecimal.ZERO)
                .totalInterest(BigDecimal.ZERO)
                .previousBalance(new BigDecimal("300"))
                .cycleStatus("OPEN")
                .build();

        when(billingCycleRepository.save(any(BillingCycle.class))).thenReturn(savedCycle);
        when(transactionRepository.saveAll(anyList())).thenReturn(unbilledTransactions);
        when(transactionRepository.findByBillingCycleCycleId(any())).thenReturn(unbilledTransactions);

        BillingCycleResponseDTO result = billingService.generateBillingCycle(cardId);

        assertNotNull(result);
        assertEquals(savedCycle.getCycleId(), result.getCycleId());
        verify(cardRepository).findById(cardId);
    }

    @Test
    void generateBillingCycle_whenNoPreviousCycle_shouldNotChargeInterest() {
        UUID cardId = card.getCardId();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(billingCycleRepository.findTopByCardCardIdOrderByCycleEndDateDesc(cardId))
                .thenReturn(Optional.empty()); // no previous cycle
        when(transactionRepository.findByCardCardIdAndBillingCycleIsNull(cardId))
                .thenReturn(List.of());
        when(paymentRepository.findPaymentsWithinCycle(any(), any(), any()))
                .thenReturn(List.of());

        BillingCycle savedCycle = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(card)
                .cycleStartDate(LocalDate.now().minusDays(30))
                .cycleEndDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .totalOutstanding(BigDecimal.ZERO)
                .minimumDue(BigDecimal.ZERO)
                .totalPurchases(BigDecimal.ZERO)
                .totalCashAdvance(BigDecimal.ZERO)
                .totalInterest(BigDecimal.ZERO)
                .previousBalance(BigDecimal.ZERO)
                .cycleStatus("OPEN")
                .build();

        when(billingCycleRepository.save(any(BillingCycle.class))).thenReturn(savedCycle);
        when(transactionRepository.saveAll(anyList())).thenReturn(List.of());
        when(transactionRepository.findByBillingCycleCycleId(any())).thenReturn(List.of());

        billingService.generateBillingCycle(cardId);

        verify(transactionService, never()).createInterest(any(), any(), any());
    }

    @Test
    void generateBillingCycle_whenPreviousCycleFullyPaid_shouldNotChargeInterest() {
        UUID cardId = card.getCardId();

        BillingCycle paidCycle = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(card)
                .cycleStatus("CLOSED")
                .cycleEndDate(LocalDate.now().minusDays(31))
                .totalOutstanding(BigDecimal.ZERO)
                .dueDate(LocalDate.now().minusDays(10))
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(billingCycleRepository.findTopByCardCardIdOrderByCycleEndDateDesc(cardId))
                .thenReturn(Optional.of(paidCycle));
        when(transactionRepository.findByCardCardIdAndBillingCycleIsNull(cardId))
                .thenReturn(List.of());
        when(paymentRepository.findPaymentsWithinCycle(any(), any(), any()))
                .thenReturn(List.of());

        BillingCycle savedCycle = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(card)
                .cycleStartDate(LocalDate.now().minusDays(30))
                .cycleEndDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .totalOutstanding(BigDecimal.ZERO)
                .minimumDue(BigDecimal.ZERO)
                .totalPurchases(BigDecimal.ZERO)
                .totalCashAdvance(BigDecimal.ZERO)
                .totalInterest(BigDecimal.ZERO)
                .previousBalance(BigDecimal.ZERO)
                .cycleStatus("OPEN")
                .build();

        when(billingCycleRepository.save(any(BillingCycle.class))).thenReturn(savedCycle);
        when(transactionRepository.saveAll(anyList())).thenReturn(List.of());
        when(transactionRepository.findByBillingCycleCycleId(any())).thenReturn(List.of());

        billingService.generateBillingCycle(cardId);

        verify(transactionService, never()).createInterest(any(), any(), any());
    }

    @Test
    void generateBillingCycle_whenPreviousCycleUnpaid_shouldChargeInterest() {
        UUID cardId = card.getCardId();

        card.setCardBalance(new BigDecimal("300.00"));
        card.setCashAdvanceBalance(BigDecimal.ZERO);

        BillingCycle unpaidCycle = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(card)
                .cycleStatus("CLOSED")
                .cycleEndDate(LocalDate.now().minusDays(31))
                .totalOutstanding(new BigDecimal("300.00"))
                .dueDate(LocalDate.now().minusDays(10))
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(billingCycleRepository.findTopByCardCardIdOrderByCycleEndDateDesc(cardId))
                .thenReturn(Optional.of(unpaidCycle));
        when(transactionRepository.findByCardCardIdAndBillingCycleIsNull(cardId))
                .thenReturn(List.of());
        when(paymentRepository.findPaymentsWithinCycle(any(), any(), any()))
                .thenReturn(List.of());

        BillingCycle savedCycle = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(card)
                .cycleStartDate(LocalDate.now().minusDays(30))
                .cycleEndDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .totalOutstanding(new BigDecimal("300.00"))
                .minimumDue(new BigDecimal("100.00"))
                .totalPurchases(BigDecimal.ZERO)
                .totalCashAdvance(BigDecimal.ZERO)
                .totalInterest(new BigDecimal("4.93"))
                .previousBalance(new BigDecimal("300.00"))
                .cycleStatus("OPEN")
                .build();

        when(billingCycleRepository.save(any(BillingCycle.class))).thenReturn(savedCycle);
        when(transactionRepository.saveAll(anyList())).thenReturn(List.of());
        when(transactionRepository.findByBillingCycleCycleId(any())).thenReturn(List.of());

        billingService.generateBillingCycle(cardId);

        verify(transactionService, atLeastOnce()).createInterest(eq(cardId), any(), any());
    }

// ── generateBillingCycle — Card Not Found ──────────────────────────────

    @Test
    void generateBillingCycle_shouldThrowWhenCardNotFound() {
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> billingService.generateBillingCycle(cardId));
    }

// ── getBillingCycle — success ───────────────────────────────────────────

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

        when(billingCycleRepository.findById(cycleId)).thenReturn(Optional.of(cycle));

        BillingCycleResponseDTO response = billingService.getBillingCycle(cardId, cycleId);

        assertNotNull(response);
        assertEquals(cycleId, response.getCycleId());
        verify(billingCycleRepository).findById(cycleId);
    }

// ── getBillingCycle — Wrong Card ID ────────────────────────────────────

    @Test
    void getBillingCycle_shouldThrowIfCycleNotBelongToCard() {
        UUID cardId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();

        BillingCycle wrongCycle = BillingCycle.builder()
                .cycleId(cycleId)
                .card(card)
                .build();

        when(billingCycleRepository.findById(cycleId)).thenReturn(Optional.of(wrongCycle));

        assertThrows(EntityNotFoundException.class,
                () -> billingService.getBillingCycle(cardId, cycleId));
    }

}