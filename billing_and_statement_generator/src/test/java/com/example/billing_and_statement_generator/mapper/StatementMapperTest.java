package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.payment.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.statement.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Statement;
import com.example.billing_and_statement_generator.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StatementMapperTest {

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private StatementMapper statementMapper;

    private Card testCard;
    private BillingCycle testBillingCycle;

    @BeforeEach
    void setUp() {
        testCard = Card.builder()
                .cardId(UUID.randomUUID())
                .availableCredit(new BigDecimal("4000.00"))
                .build();

        testBillingCycle = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(testCard)
                .cycleStartDate(LocalDate.now().minusDays(30))
                .cycleEndDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .build();
    }

// ── toEntity() tests ────────────────────────────────────────────────────

    @Test
    void givenValidInputs_whenToEntityCalled_thenReturnsStatementEntity() {
        Statement result = statementMapper.toEntity(
                testCard, testBillingCycle,
                new BigDecimal("1020.00"), new BigDecimal("1020.00"),
                new BigDecimal("100.00"), new BigDecimal("20.00"),
                new BigDecimal("1020.00"), new BigDecimal("50.00"),
                new BigDecimal("20.00"), new BigDecimal("1020.00")
        );

        assertThat(result).isNotNull();
        assertThat(result.getStatementId()).isNotNull();
        assertThat(result.getCard()).isEqualTo(testCard);
        assertThat(result.getBillingCycle()).isEqualTo(testBillingCycle);
        assertThat(result.getStatementDate()).isEqualTo(LocalDate.now());
        assertThat(result.getDueDate()).isEqualTo(testBillingCycle.getDueDate());
        assertThat(result.getStatementBalance()).isEqualByComparingTo(new BigDecimal("1020.00"));
        assertThat(result.getMinimumDue()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.getStatementStatus()).isEqualTo(Statement.StatementStatus.GENERATED);
    }

    @Test
    void givenZeroValues_whenToEntityCalled_thenReturnsStatementWithZeros() {
        Statement result = statementMapper.toEntity(
                testCard, testBillingCycle,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );

        assertThat(result.getStatementBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getStatementStatus()).isEqualTo(Statement.StatementStatus.GENERATED);
    }

// ── toGenerateResponseDTO() tests ───────────────────────────────────────

    @Test
    void givenStatement_whenToGenerateResponseDTOCalled_thenReturnsCorrectDTO() {
        Statement statement = buildStatement(Statement.StatementStatus.GENERATED);

        GenerateStatementResponseDTO result = statementMapper.toGenerateResponseDTO(statement);

        assertThat(result).isNotNull();
        assertThat(result.getStatementId()).isEqualTo(statement.getStatementId().toString());
        assertThat(result.getCardId()).isEqualTo(testCard.getCardId().toString());
        assertThat(result.getCycleId()).isEqualTo(testBillingCycle.getCycleId().toString());
        assertThat(result.getStatementStatus()).isEqualTo("GENERATED");
        assertThat(result.getMessage()).isEqualTo("Statement generated successfully");
    }

// ── toRetrieveResponseDTO() tests ───────────────────────────────────────

    @Test
    void givenStatementWithTransactionsAndPayments_whenToRetrieveResponseDTOCalled_thenReturnsCorrectDTO() {
        Statement statement = buildStatement(Statement.StatementStatus.GENERATED);

        List<CreateTransactionResponseDTO> transactions = List.of(
                CreateTransactionResponseDTO.builder()
                        .transactionId(UUID.randomUUID())
                        .cardId(testCard.getCardId())
                        .amount(new BigDecimal("500.00"))
                        .transactionType(Transaction.transactionType.PURCHASE)
                        .merchantName("Amazon")
                        .status(Transaction.Status.SENT)
                        .build()
        );

        List<RetrievePaymentHistoryDTO> payments = List.of(
                RetrievePaymentHistoryDTO.builder()
                        .paymentId(UUID.randomUUID().toString())
                        .cardId(testCard.getCardId().toString())
                        .amountPaid("500.00")
                        .paymentType("PARTIAL")
                        .paymentStatus("SUCCESS")
                        .build()
        );

        BigDecimal amountPaid = new BigDecimal("500.00");

        RetrieveStatementResponseDTO result =
                statementMapper.toRetrieveResponseDTO(statement, transactions, payments, amountPaid);

        assertThat(result).isNotNull();
        assertThat(result.getStatementId()).isEqualTo(statement.getStatementId().toString());
        assertThat(result.getCardId()).isEqualTo(testCard.getCardId().toString());
        assertThat(result.getStatementBalance()).isEqualTo("1020.00");
        assertThat(result.getAvailableCredit()).isEqualTo("4000.00");
        assertThat(result.getAmountPaid()).isEqualTo("500.00");
        assertThat(result.getStatementStatus()).isEqualTo("GENERATED");
        assertThat(result.getTransactions()).hasSize(1);
        assertThat(result.getTransactions().get(0).getMerchantName()).isEqualTo("Amazon");
        assertThat(result.getPayments()).hasSize(1);
        assertThat(result.getPayments().get(0).getAmountPaid()).isEqualTo("500.00");
    }

    @Test
    void givenStatementWithEmptyLists_whenToRetrieveResponseDTOCalled_thenReturnsEmptyLists() {
        Statement statement = buildStatement(Statement.StatementStatus.GENERATED);

        RetrieveStatementResponseDTO result =
                statementMapper.toRetrieveResponseDTO(statement, List.of(), List.of(), BigDecimal.ZERO);

        assertThat(result.getTransactions()).isEmpty();
        assertThat(result.getPayments()).isEmpty();
        assertThat(result.getAmountPaid()).isEqualTo("0");
        assertThat(result.getAvailableCredit()).isEqualTo("4000.00");
    }

    @Test
    void givenPaidStatement_whenToRetrieveResponseDTOCalled_thenReturnsCorrectStatus() {
        Statement statement = Statement.builder()
                .statementId(UUID.randomUUID())
                .card(testCard)
                .billingCycle(testBillingCycle)
                .statementDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .billingStartDate(LocalDate.now().minusDays(30))
                .billingEndDate(LocalDate.now())
                .statementBalance(new BigDecimal("1020.00"))
                .remainingStatementBalance(BigDecimal.ZERO)
                .minimumDue(new BigDecimal("100.00"))
                .totalInterest(new BigDecimal("20.00"))
                .totalOutstanding(new BigDecimal("1020.00"))
                .totalFeeApplied(new BigDecimal("50.00"))
                .cashAdvanceFee(new BigDecimal("20.00"))
                .carryForwardBalance(BigDecimal.ZERO)
                .statementStatus(Statement.StatementStatus.PAID)
                .build();

        RetrieveStatementResponseDTO result =
                statementMapper.toRetrieveResponseDTO(statement, List.of(), List.of(), new BigDecimal("1020.00"));

        assertThat(result.getStatementStatus()).isEqualTo("PAID");
        assertThat(result.getAmountPaid()).isEqualTo("1020.00");
        assertThat(result.getTransactions()).isEmpty();
        assertThat(result.getPayments()).isEmpty();
    }

    @Test
    void givenNullAvailableCredit_whenToRetrieveResponseDTOCalled_thenDefaultsToZero() {
        Card cardNoCredit = Card.builder()
                .cardId(UUID.randomUUID())
                .availableCredit(null)
                .build();

        BillingCycle cycle = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(cardNoCredit)
                .cycleStartDate(LocalDate.now().minusDays(30))
                .cycleEndDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .build();

        Statement statement = Statement.builder()
                .statementId(UUID.randomUUID())
                .card(cardNoCredit)
                .billingCycle(cycle)
                .statementDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .billingStartDate(LocalDate.now().minusDays(30))
                .billingEndDate(LocalDate.now())
                .statementBalance(new BigDecimal("1020.00"))
                .remainingStatementBalance(new BigDecimal("1020.00"))
                .minimumDue(new BigDecimal("100.00"))
                .totalInterest(BigDecimal.ZERO)
                .totalOutstanding(new BigDecimal("1020.00"))
                .totalFeeApplied(BigDecimal.ZERO)
                .cashAdvanceFee(BigDecimal.ZERO)
                .carryForwardBalance(new BigDecimal("1020.00"))
                .statementStatus(Statement.StatementStatus.GENERATED)
                .build();

        RetrieveStatementResponseDTO result =
                statementMapper.toRetrieveResponseDTO(statement, List.of(), List.of(), BigDecimal.ZERO);

        assertThat(result.getAvailableCredit()).isEqualTo("0.00");
    }

    // Helper
    private Statement buildStatement(Statement.StatementStatus status) {
        return Statement.builder()
                .statementId(UUID.randomUUID())
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
                .statementStatus(status)
                .build();
    }

}