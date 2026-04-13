package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.statement.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.statement.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Statement;
import com.example.billing_and_statement_generator.mapper.PaymentMapper;
import com.example.billing_and_statement_generator.mapper.StatementMapper;
import com.example.billing_and_statement_generator.mapper.TransactionMapper;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.PaymentRepository;
import com.example.billing_and_statement_generator.repository.StatementRepository;
import com.example.billing_and_statement_generator.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class StatementServiceBDDTest {

    @Mock StatementRepository statementRepository;
    @Mock CardRepository cardRepository;
    @Mock BillingCycleRepository billingCycleRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock StatementMapper statementMapper;
    @Mock TransactionMapper transactionMapper;
    @Mock PaymentMapper paymentMapper;
    @Mock ObjectMapper objectMapper;

    @InjectMocks
    StatementService statementService;

    private UUID cardId;
    private UUID cycleId;
    private UUID statementId;
    private Card card;
    private BillingCycle billingCycle;
    private Statement statement;
    private String snapshotJson;

// GIVEN
// WHEN
// THEN

    @BeforeEach
    void setup() {
        cardId = UUID.randomUUID();
        cycleId = UUID.randomUUID();
        statementId = UUID.randomUUID();

        card = Card.builder()
                .cardId(cardId)
                .cardBalance(new BigDecimal("1020.00"))
                .cashAdvanceBalance(BigDecimal.ZERO)
                .availableCredit(new BigDecimal("4000.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashAdvanceFeeRate(new BigDecimal("0.02"))
                .build();

        billingCycle = BillingCycle.builder()
                .cycleId(cycleId)
                .card(card)
                .cycleStartDate(LocalDate.now().minusDays(30))
                .cycleEndDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .totalOutstanding(new BigDecimal("1020.00"))
                .totalInterest(new BigDecimal("20.00"))
                .minimumDue(new BigDecimal("100.00"))
                .build();

        snapshotJson = "{\"statementId\":\"" + statementId + "\"," +
                "\"statementStatus\":\"GENERATED\"," +
                "\"availableCredit\":\"4000.00\"," +
                "\"amountPaid\":\"0.00\"," +
                "\"transactions\":[]," +
                "\"payments\":[]}";

        statement = Statement.builder()
                .statementId(statementId)
                .card(card)
                .billingCycle(billingCycle)
                .statementDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .billingStartDate(LocalDate.now().minusDays(30))
                .billingEndDate(LocalDate.now())
                .statementBalance(new BigDecimal("1020.00"))
                .remainingStatementBalance(new BigDecimal("1020.00"))
                .minimumDue(new BigDecimal("100.00"))
                .totalInterest(new BigDecimal("20.00"))
                .totalOutstanding(new BigDecimal("1020.00"))
                .totalFeeApplied(BigDecimal.ZERO)
                .cashAdvanceFee(BigDecimal.ZERO)
                .carryForwardBalance(new BigDecimal("1020.00"))
                .statementStatus(Statement.StatementStatus.GENERATED)
                .statementSnapshot(snapshotJson)
                .build();
    }

    @Test
    void shouldGenerateStatementSuccessfully() throws Exception {
        // GIVEN
        RetrieveStatementResponseDTO snapshotDTO = RetrieveStatementResponseDTO.builder()
                .statementId(statementId.toString())
                .statementStatus("GENERATED")
                .transactions(List.of())
                .payments(List.of())
                .build();

        GenerateStatementResponseDTO expectedResponse = GenerateStatementResponseDTO.builder()
                .statementId(statementId.toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .statementStatus("GENERATED")
                .message("Statement generated successfully")
                .build();

        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        given(billingCycleRepository.findById(cycleId)).willReturn(Optional.of(billingCycle));
        given(statementRepository.existsByCycleId(cycleId)).willReturn(false);
        given(statementMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(statement);
        given(transactionRepository.findByBillingCycleCycleId(cycleId)).willReturn(List.of());
        given(paymentRepository.findByCycleId(cycleId)).willReturn(List.of());
        given(statementMapper.toRetrieveResponseDTO(any(), any(), any(), any())).willReturn(snapshotDTO);
        given(objectMapper.writeValueAsString(snapshotDTO)).willReturn(snapshotJson);
        given(statementRepository.save(any())).willReturn(statement);
        given(statementMapper.toGenerateResponseDTO(statement)).willReturn(expectedResponse);

        // WHEN
        GenerateStatementResponseDTO result = statementService.generateStatement(
                GenerateStatementRequestDTO.builder()
                        .cardId(cardId.toString())
                        .cycleId(cycleId.toString())
                        .build());

        // THEN
        assertEquals("GENERATED", result.getStatementStatus());
        assertEquals("Statement generated successfully", result.getMessage());
        then(statementRepository).should().save(any(Statement.class));
    }

    @Test
    void shouldThrowExceptionWhenCardNotFoundForGenerate() {
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> statementService.generateStatement(
                        GenerateStatementRequestDTO.builder()
                                .cardId(cardId.toString())
                                .cycleId(cycleId.toString())
                                .build()),
                "Expected RuntimeException when card not found");

        then(statementRepository).should(never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenBillingCycleNotFound() {
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        given(billingCycleRepository.findById(cycleId)).willReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> statementService.generateStatement(
                        GenerateStatementRequestDTO.builder()
                                .cardId(cardId.toString())
                                .cycleId(cycleId.toString())
                                .build()),
                "Expected RuntimeException when billing cycle not found");

        then(statementRepository).should(never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenStatementAlreadyExists() {
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        given(billingCycleRepository.findById(cycleId)).willReturn(Optional.of(billingCycle));
        given(statementRepository.existsByCycleId(cycleId)).willReturn(true);

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> statementService.generateStatement(
                        GenerateStatementRequestDTO.builder()
                                .cardId(cardId.toString())
                                .cycleId(cycleId.toString())
                                .build()),
                "Expected RuntimeException when statement already exists");

        then(statementRepository).should(never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCycleDoesNotBelongToCard() {
        // GIVEN
        Card differentCard = Card.builder().cardId(UUID.randomUUID()).build();
        BillingCycle wrongCycle = BillingCycle.builder()
                .cycleId(cycleId).card(differentCard).build();

        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        given(billingCycleRepository.findById(cycleId)).willReturn(Optional.of(wrongCycle));

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> statementService.generateStatement(
                        GenerateStatementRequestDTO.builder()
                                .cardId(cardId.toString())
                                .cycleId(cycleId.toString())
                                .build()),
                "Expected RuntimeException when cycle does not belong to card");

        then(statementRepository).should(never()).save(any());
    }

    @Test
    void shouldReturnSnapshotWhenGetStatementCalled() throws Exception {
        // GIVEN
        RetrieveStatementResponseDTO snapshotDTO = RetrieveStatementResponseDTO.builder()
                .statementId(statementId.toString())
                .cardId(cardId.toString())
                .cycleId(cycleId.toString())
                .statementStatus("GENERATED")
                .availableCredit("4000.00")
                .amountPaid("0.00")
                .transactions(List.of())
                .payments(List.of())
                .build();

        given(statementRepository.findById(statementId)).willReturn(Optional.of(statement));
        given(objectMapper.readValue(snapshotJson, RetrieveStatementResponseDTO.class))
                .willReturn(snapshotDTO);

        // WHEN
        RetrieveStatementResponseDTO result = statementService.getStatement(statementId);

        // THEN
        assertEquals(statementId.toString(), result.getStatementId());
        assertEquals("GENERATED", result.getStatementStatus());
        assertNotNull(result.getTransactions());
        assertNotNull(result.getPayments());
        then(transactionRepository).should(never()).findByBillingCycleCycleId(any());
        then(paymentRepository).should(never()).findByCycleId(any());
    }

    @Test
    void shouldThrowExceptionWhenStatementNotFound() {
        // GIVEN
        given(statementRepository.findById(statementId)).willReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> statementService.getStatement(statementId),
                "Expected RuntimeException when statement not found");

        then(transactionRepository).should(never()).findByBillingCycleCycleId(any());
    }

    /*
     * Other tests to consider:
     * - shouldCalculateRemainingBalanceAfterPayments
     * - shouldReturnFrozenSnapshotAfterNewTransactions
     * - shouldIncludeCorrectAvailableCredit
     * - shouldHandleNullAvailableCredit
     */

}