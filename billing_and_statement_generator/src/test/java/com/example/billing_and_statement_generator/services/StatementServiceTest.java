package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.statement.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.statement.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Statement;
import com.example.billing_and_statement_generator.entity.Transaction;
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

    // generateStatement() tests

    @Test
    void givenValidRequest_whenGenerateStatementCalled_thenReturnsGenerateResponse() {
        GenerateStatementRequestDTO dto = buildGenerateDto();

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
        when(paymentRepository.findTotalPaidByCycleId(cycleId)).thenReturn(BigDecimal.ZERO);
        when(statementMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(testStatement);
        when(statementRepository.save(any(Statement.class))).thenReturn(testStatement);
        when(statementMapper.toGenerateResponseDTO(testStatement)).thenReturn(expectedResponse);

        GenerateStatementResponseDTO result = statementService.generateStatement(dto);

        assertThat(result).isNotNull();
        assertThat(result.getStatementStatus()).isEqualTo("GENERATED");
        assertThat(result.getMessage()).isEqualTo("Statement generated successfully");

        verify(cardRepository).findById(cardId);
        verify(billingCycleRepository).findById(cycleId);
        verify(statementRepository).save(any(Statement.class));
    }

    @Test
    void givenNonExistentCard_whenGenerateStatementCalled_thenThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> statementService.generateStatement(buildGenerateDto()));

        verify(statementRepository, never()).save(any());
    }

    @Test
    void givenNonExistentCycle_whenGenerateStatementCalled_thenThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(billingCycleRepository.findById(cycleId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> statementService.generateStatement(buildGenerateDto()));

        verify(statementRepository, never()).save(any());
    }

    @Test
    void givenCycleNotBelongingToCard_whenGenerateStatementCalled_thenThrowsException() {
        Card differentCard = Card.builder().cardId(UUID.randomUUID()).build();
        BillingCycle wrongCycle = BillingCycle.builder()
                .cycleId(cycleId).card(differentCard).build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(billingCycleRepository.findById(cycleId)).thenReturn(Optional.of(wrongCycle));

        assertThrows(RuntimeException.class,
                () -> statementService.generateStatement(buildGenerateDto()));

        verify(statementRepository, never()).save(any());
    }

    @Test
    void givenStatementAlreadyExists_whenGenerateStatementCalled_thenThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(billingCycleRepository.findById(cycleId)).thenReturn(Optional.of(testBillingCycle));
        when(statementRepository.existsByCycleId(cycleId)).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> statementService.generateStatement(buildGenerateDto()));

        verify(statementRepository, never()).save(any());
    }

    // getStatement() tests

    @Test
    void givenValidCardAndCycleId_whenGetStatementCalled_thenReturnsStatementWithTransactions() {
        Transaction tx = Transaction.builder()
                .transactionId(UUID.randomUUID())
                .card(testCard)
                .billingCycle(testBillingCycle)
                .transactionType(Transaction.transactionType.PURCHASE)
                .amount(new BigDecimal("500.00"))
                .merchantName("Amazon")
                .status(Transaction.Status.SENT)
                .build();

        CreateTransactionResponseDTO txDto = CreateTransactionResponseDTO.builder()
                .transactionId(tx.getTransactionId())
                .cardId(cardId)
                .amount(new BigDecimal("500.00"))
                .merchantName("Amazon")
                .build();

        RetrieveStatementResponseDTO expectedResponse = RetrieveStatementResponseDTO.builder()
                .statementId(statementId.toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .statementStatus("GENERATED")
                .transactions(List.of(txDto))
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(statementRepository.findByCycleId(cycleId)).thenReturn(Optional.of(testStatement));
        when(transactionRepository.findByBillingCycleCycleId(cycleId)).thenReturn(List.of(tx));
        when(transactionMapper.toResponse(tx)).thenReturn(txDto);
        when(statementMapper.toRetrieveResponseDTO(eq(testStatement), anyList()))
                .thenReturn(expectedResponse);

        RetrieveStatementResponseDTO result = statementService.getStatement(cardId, cycleId);

        assertThat(result).isNotNull();
        assertThat(result.getStatementId()).isEqualTo(statementId.toString());
        assertThat(result.getStatementStatus()).isEqualTo("GENERATED");
        assertThat(result.getTransactions()).hasSize(1);
        assertThat(result.getTransactions().get(0).getMerchantName()).isEqualTo("Amazon");

        verify(transactionRepository).findByBillingCycleCycleId(cycleId);
        verify(statementMapper).toRetrieveResponseDTO(eq(testStatement), anyList());
    }

    @Test
    void givenCycleWithNoTransactions_whenGetStatementCalled_thenReturnsEmptyTransactionList() {
        RetrieveStatementResponseDTO expectedResponse = RetrieveStatementResponseDTO.builder()
                .statementId(statementId.toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .statementStatus("GENERATED")
                .transactions(List.of())
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(statementRepository.findByCycleId(cycleId)).thenReturn(Optional.of(testStatement));
        when(transactionRepository.findByBillingCycleCycleId(cycleId)).thenReturn(List.of());
        when(statementMapper.toRetrieveResponseDTO(eq(testStatement), eq(List.of())))
                .thenReturn(expectedResponse);

        RetrieveStatementResponseDTO result = statementService.getStatement(cardId, cycleId);

        assertThat(result.getTransactions()).isEmpty();
        verify(transactionRepository).findByBillingCycleCycleId(cycleId);
    }

    @Test
    void givenNonExistentCard_whenGetStatementCalled_thenThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> statementService.getStatement(cardId, cycleId));

        verify(statementRepository, never()).findByCycleId(any());
        verify(transactionRepository, never()).findByBillingCycleCycleId(any());
    }

    @Test
    void givenNonExistentStatement_whenGetStatementCalled_thenThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(statementRepository.findByCycleId(cycleId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> statementService.getStatement(cardId, cycleId));

        verify(transactionRepository, never()).findByBillingCycleCycleId(any());
    }

    @Test
    void givenStatementNotBelongingToCard_whenGetStatementCalled_thenThrowsException() {
        Card differentCard = Card.builder().cardId(UUID.randomUUID()).build();
        Statement wrongStatement = Statement.builder()
                .statementId(statementId)
                .card(differentCard)
                .billingCycle(testBillingCycle)
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(statementRepository.findByCycleId(cycleId)).thenReturn(Optional.of(wrongStatement));

        assertThrows(RuntimeException.class,
                () -> statementService.getStatement(cardId, cycleId));

        verify(transactionRepository, never()).findByBillingCycleCycleId(any());
    }

    // Helper

    private GenerateStatementRequestDTO buildGenerateDto() {
        return GenerateStatementRequestDTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .build();
    }
}