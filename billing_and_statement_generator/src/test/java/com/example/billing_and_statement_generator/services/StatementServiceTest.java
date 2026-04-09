package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.payment.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.statement.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Payment;
import com.example.billing_and_statement_generator.entity.Statement;
import com.example.billing_and_statement_generator.entity.Transaction;
import com.example.billing_and_statement_generator.mapper.PaymentMapper;
import com.example.billing_and_statement_generator.mapper.StatementMapper;
import com.example.billing_and_statement_generator.mapper.TransactionMapper;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.PaymentRepository;
import com.example.billing_and_statement_generator.repository.StatementRepository;
import com.example.billing_and_statement_generator.repository.TransactionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock private StatementRepository statementRepository;
    @Mock private CardRepository cardRepository;
    @Mock private BillingCycleRepository billingCycleRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private StatementMapper statementMapper;
    @Mock private TransactionMapper transactionMapper;
    @Mock private PaymentMapper paymentMapper;

    @InjectMocks
    private StatementService statementService;

    private UUID cardId;
    private UUID cycleId;
    private UUID statementId;
    private Card testCard;
    private BillingCycle testBillingCycle;
    private Statement testStatement;

    @BeforeEach
    void setUp() {
        cardId = UUID.randomUUID();
        cycleId = UUID.randomUUID();
        statementId = UUID.randomUUID();

        testCard = Card.builder()
                .cardId(cardId)
                .cashAdvanceFeeRate(new BigDecimal("0.02"))
                .lateFeeAmount(new BigDecimal("50.00"))
                .availableCredit(new BigDecimal("4000.00"))
                .cardBalance(new BigDecimal("1020.00"))
                .cashAdvanceBalance(BigDecimal.ZERO)
                .build();

        testBillingCycle = BillingCycle.builder()
                .cycleId(cycleId)
                .card(testCard)
                .cycleStartDate(LocalDate.now().minusDays(30))
                .cycleEndDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .totalPurchases(new BigDecimal("1000.00"))
                .totalCashAdvance(BigDecimal.ZERO)
                .totalInterest(new BigDecimal("20.00"))
                .totalOutstanding(new BigDecimal("1020.00"))
                .minimumDue(new BigDecimal("100.00"))
                .build();

        testStatement = Statement.builder()
                .statementId(statementId)
                .card(testCard)
                .billingCycle(testBillingCycle)
                .statementDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .billingStartDate(LocalDate.now().minusDays(30))
                .billingEndDate(LocalDate.now())
                .statementBalance(new BigDecimal("1020.00"))
                .remainingStatementBalance(new BigDecimal("1020.00"))
                .minimumDue(new BigDecimal("100.00"))
                .totalInterest(new BigDecimal("20.00"))
                .totalOutstanding(new BigDecimal("1020.00"))
                .totalFeeApplied(new BigDecimal("50.00"))
                .cashAdvanceFee(new BigDecimal("20.00"))
                .carryForwardBalance(new BigDecimal("1020.00"))
                .statementStatus(Statement.StatementStatus.GENERATED)
                .build();
    }

// ── generateStatement() tests ───────────────────────────────────────────

    @Test
    void givenValidRequest_whenGenerateStatementCalled_thenReturnsGenerateResponse() {
        GenerateStatementResponseDTO expectedResponse = GenerateStatementResponseDTO.builder()
                .statementId(statementId.toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .statementStatus("GENERATED")
                .message("Statement generated successfully")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(billingCycleRepository.findById(cycleId)).thenReturn(Optional.of(testBillingCycle));
        when(statementRepository.existsByCycleId(cycleId)).thenReturn(false);
        when(statementMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(testStatement);
        when(statementRepository.save(any(Statement.class))).thenReturn(testStatement);
        when(statementMapper.toGenerateResponseDTO(testStatement)).thenReturn(expectedResponse);

        GenerateStatementResponseDTO result = statementService.generateStatement(buildGenerateDto());

        assertThat(result).isNotNull();
        assertThat(result.getStatementStatus()).isEqualTo("GENERATED");
        assertThat(result.getMessage()).isEqualTo("Statement generated successfully");
        assertThat(result.getStatementId()).isEqualTo(statementId.toString());

        verify(cardRepository).findById(cardId);
        verify(billingCycleRepository).findById(cycleId);
        verify(statementRepository).save(any(Statement.class));
        verify(statementMapper).toGenerateResponseDTO(testStatement);
    }

    @Test
    void givenNonExistentCard_whenGenerateStatementCalled_thenThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> statementService.generateStatement(buildGenerateDto()));

        verify(statementRepository, never()).save(any());
    }

    @Test
    void givenNonExistentStatementId_whenGetStatementCalled_thenThrowsException() {
        when(statementRepository.findById(statementId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> statementService.getStatement(statementId));

        verify(transactionRepository, never()).findByBillingCycleCycleId(any());
        verify(paymentRepository, never()).findByCycleId(any());
    }

    // Helper
    private GenerateStatementRequestDTO buildGenerateDto() {
        return GenerateStatementRequestDTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .build();
    }

}