package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Statement;
import com.example.billing_and_statement_generator.mapper.StatementMapper;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.PaymentRepository;
import com.example.billing_and_statement_generator.repository.StatementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock
    private StatementRepository statementRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private BillingCycleRepository billingCycleRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StatementMapper statementMapper;

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

    // ── generateStatement() tests ───────────────────────────────────

    @Test
    void givenValidRequest_whenGenerateStatementCalled_thenReturnsGenerateResponse() {
        GenerateStatementRequestDTO dto = GenerateStatementRequestDTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .build();

        GenerateStatementResponseDTO expectedResponse =
                GenerateStatementResponseDTO.builder()
                        .statementId(statementId.toString())
                        .cardId(cardId.toString())
                        .cycleId(cycleId.toString())
                        .statementStatus("GENERATED")
                        .message("Statement generated successfully")
                        .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(billingCycleRepository.findById(cycleId))
                .thenReturn(Optional.of(testBillingCycle));
        when(statementRepository.existsByCycleId(cycleId)).thenReturn(false);
        when(paymentRepository.findTotalPaidByCycleId(cycleId))
                .thenReturn(BigDecimal.ZERO);
        when(statementMapper.toEntity(any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any()))
                .thenReturn(testStatement);
        when(statementRepository.save(any(Statement.class)))
                .thenReturn(testStatement);
        when(statementMapper.toGenerateResponseDTO(testStatement))
                .thenReturn(expectedResponse);

        GenerateStatementResponseDTO result =
                statementService.generateStatement(dto);

        assertThat(result).isNotNull();
        assertThat(result.getStatementStatus()).isEqualTo("GENERATED");
        assertThat(result.getMessage()).isEqualTo("Statement generated successfully");

        verify(cardRepository).findById(cardId);
        verify(billingCycleRepository).findById(cycleId);
        verify(statementRepository).save(any(Statement.class));
    }

    @Test
    void givenNonExistentCard_whenGenerateStatementCalled_thenThrowsException() {
        GenerateStatementRequestDTO dto = GenerateStatementRequestDTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                statementService.generateStatement(dto));

        verify(cardRepository).findById(cardId);
        verify(statementRepository, never()).save(any());
    }

    @Test
    void givenNonExistentCycle_whenGenerateStatementCalled_thenThrowsException() {
        GenerateStatementRequestDTO dto = GenerateStatementRequestDTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(billingCycleRepository.findById(cycleId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                statementService.generateStatement(dto));

        verify(billingCycleRepository).findById(cycleId);
        verify(statementRepository, never()).save(any());
    }

    @Test
    void givenCycleNotBelongingToCard_whenGenerateStatementCalled_thenThrowsException() {
        Card differentCard = Card.builder()
                .cardId(UUID.randomUUID())
                .build();

        BillingCycle cycleWithDifferentCard = BillingCycle.builder()
                .cycleId(cycleId)
                .card(differentCard)
                .build();

        GenerateStatementRequestDTO dto = GenerateStatementRequestDTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(billingCycleRepository.findById(cycleId))
                .thenReturn(Optional.of(cycleWithDifferentCard));

        assertThrows(RuntimeException.class, () ->
                statementService.generateStatement(dto));

        verify(statementRepository, never()).save(any());
    }

    @Test
    void givenStatementAlreadyExists_whenGenerateStatementCalled_thenThrowsException() {
        GenerateStatementRequestDTO dto = GenerateStatementRequestDTO.builder()
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(billingCycleRepository.findById(cycleId))
                .thenReturn(Optional.of(testBillingCycle));
        when(statementRepository.existsByCycleId(cycleId)).thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                statementService.generateStatement(dto));

        verify(statementRepository, never()).save(any());
    }

    // ── getStatement() tests ────────────────────────────────────────

    @Test
    void givenValidCardAndCycleId_whenGetStatementCalled_thenReturnsStatement() {
        RetrieveStatementResponseDTO expectedResponse =
                RetrieveStatementResponseDTO.builder()
                        .statementId(statementId.toString())
                        .cardId(cardId.toString())
                        .cycleId(cycleId.toString())
                        .statementStatus("GENERATED")
                        .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(statementRepository.findByCycleId(cycleId))
                .thenReturn(Optional.of(testStatement));
        when(statementMapper.toRetrieveResponseDTO(testStatement))
                .thenReturn(expectedResponse);

        RetrieveStatementResponseDTO result =
                statementService.getStatement(cardId, cycleId);

        assertThat(result).isNotNull();
        assertThat(result.getStatementId()).isEqualTo(statementId.toString());
        assertThat(result.getStatementStatus()).isEqualTo("GENERATED");

        verify(cardRepository).findById(cardId);
        verify(statementRepository).findByCycleId(cycleId);
    }

    @Test
    void givenNonExistentCard_whenGetStatementCalled_thenThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                statementService.getStatement(cardId, cycleId));

        verify(cardRepository).findById(cardId);
        verify(statementRepository, never()).findByCycleId(any());
    }

    @Test
    void givenNonExistentStatement_whenGetStatementCalled_thenThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(statementRepository.findByCycleId(cycleId))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                statementService.getStatement(cardId, cycleId));

        verify(statementRepository).findByCycleId(cycleId);
    }

    @Test
    void givenStatementNotBelongingToCard_whenGetStatementCalled_thenThrowsException() {
        Card differentCard = Card.builder()
                .cardId(UUID.randomUUID())
                .build();

        Statement statementWithDifferentCard = Statement.builder()
                .statementId(statementId)
                .card(differentCard)
                .billingCycle(testBillingCycle)
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(statementRepository.findByCycleId(cycleId))
                .thenReturn(Optional.of(statementWithDifferentCard));

        assertThrows(RuntimeException.class, () ->
                statementService.getStatement(cardId, cycleId));
    }
}