package com.example.billing_and_statement_generator.mapper;

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

// toEntity() tests

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
        assertThat(result.getBillingStartDate()).isEqualTo(testBillingCycle.getCycleStartDate());
        assertThat(result.getBillingEndDate()).isEqualTo(testBillingCycle.getCycleEndDate());
        assertThat(result.getStatementBalance()).isEqualByComparingTo(new BigDecimal("1020.00"));
        assertThat(result.getMinimumDue()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.getTotalInterest()).isEqualByComparingTo(new BigDecimal("20.00"));
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
        assertThat(result.getMinimumDue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getStatementStatus()).isEqualTo(Statement.StatementStatus.GENERATED);
    }

    @Test
    void givenValidInputs_whenToEntityCalled_thenStatementStatusIsGenerated() {
        Statement result = statementMapper.toEntity(
                testCard, testBillingCycle,
                new BigDecimal("500.00"), new BigDecimal("500.00"),
                new BigDecimal("25.00"), new BigDecimal("10.00"),
                new BigDecimal("500.00"), new BigDecimal("5.00"),
                new BigDecimal("10.00"), new BigDecimal("500.00")
        );

        assertThat(result.getStatementStatus()).isEqualTo(Statement.StatementStatus.GENERATED);
        assertThat(result.getStatementId()).isNotNull();
    }

// toRetrieveResponseDTO() tests

    @Test
    void givenStatementWithTransactions_whenToRetrieveResponseDTOCalled_thenReturnsCorrectDTO() {
        UUID statementId = UUID.randomUUID();
        Statement statement = Statement.builder()
                .statementId(statementId)
                .card(testCard)
                .billingCycle(testBillingCycle)
                .statementDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(21))
                .billingStartDate(LocalDate.now().minusDays(30))
                .billingEndDate(LocalDate.now())
                .statementBalance(new BigDecimal("1020.00"))
                .remainingStatementBalance(new BigDecimal("500.00"))
                .minimumDue(new BigDecimal("100.00"))
                .totalInterest(new BigDecimal("20.00"))
                .totalOutstanding(new BigDecimal("1020.00"))
                .totalFeeApplied(new BigDecimal("50.00"))
                .cashAdvanceFee(new BigDecimal("20.00"))
                .carryForwardBalance(new BigDecimal("500.00"))
                .statementStatus(Statement.StatementStatus.UNPAID)
                .build();

        List&lt;CreateTransactionResponseDTO&gt; transactions = List.of(
                CreateTransactionResponseDTO.builder()
                        .transactionId(UUID.randomUUID())
                        .cardId(testCard.getCardId())
                        .amount(new BigDecimal("500.00"))
                        .transactionType(Transaction.transactionType.PURCHASE)
                        .merchantName("Amazon")
                        .status(Transaction.Status.SENT)
                        .build(),
                CreateTransactionResponseDTO.builder()
                        .transactionId(UUID.randomUUID())
                        .cardId(testCard.getCardId())
                        .amount(new BigDecimal("520.00"))
                        .transactionType(Transaction.transactionType.PURCHASE)
                        .merchantName("Best Buy")
                        .status(Transaction.Status.SENT)
                        .build()
        );

        RetrieveStatementResponseDTO result =
                statementMapper.toRetrieveResponseDTO(statement, transactions);

        assertThat(result).isNotNull();
        assertThat(result.getStatementId()).isEqualTo(statementId.toString());
        assertThat(result.getCardId()).isEqualTo(testCard.getCardId().toString());
        assertThat(result.getCycleId()).isEqualTo(testBillingCycle.getCycleId().toString());
        assertThat(result.getStatementBalance()).isEqualTo("1020.00");
        assertThat(result.getRemainingStatementBalance()).isEqualTo("500.00");
        assertThat(result.getMinimumDue()).isEqualTo("100.00");
        assertThat(result.getTotalInterest()).isEqualTo("20.00");
        assertThat(result.getStatementStatus()).isEqualTo("UNPAID");
        assertThat(result.getAvailableCredit()).isEqualTo("4000.00");
        assertThat(result.getMessage()).isEqualTo("Statement generated successfully");
        assertThat(result.getTransactions()).hasSize(2);
        assertThat(result.getTransactions().get(0).getMerchantName()).isEqualTo("Amazon");
        assertThat(result.getTransactions().get(1).getMerchantName()).isEqualTo("Best Buy");
    }

    @Test
    void givenStatementWithEmptyTransactions_whenToRetrieveResponseDTOCalled_thenTransactionsIsEmpty() {
        Statement statement = buildStatement(Statement.StatementStatus.GENERATED);

        RetrieveStatementResponseDTO result =
                statementMapper.toRetrieveResponseDTO(statement, List.of());

        assertThat(result.getTransactions()).isNotNull();
        assertThat(result.getTransactions()).isEmpty();
        assertThat(result.getAvailableCredit()).isEqualTo("4000.00");
        assertThat(result.getMessage()).isEqualTo("Statement generated successfully");
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
                statementMapper.toRetrieveResponseDTO(statement, List.of());

        assertThat(result.getStatementStatus()).isEqualTo("PAID");
        assertThat(result.getRemainingStatementBalance()).isEqualTo("0");
        assertThat(result.getAvailableCredit()).isEqualTo("4000.00");
        assertThat(result.getTransactions()).isEmpty();
    }

    @Test
    void givenCardWithNullAvailableCredit_whenToRetrieveResponseDTOCalled_thenAvailableCreditIsZero() {
        Card cardWithNullCredit = Card.builder()
                .cardId(UUID.randomUUID())
                .availableCredit(null)
                .build();

        testBillingCycle.setCard(cardWithNullCredit);

        Statement statement = Statement.builder()
                .statementId(UUID.randomUUID())
                .card(cardWithNullCredit)
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
                .totalFeeApplied(BigDecimal.ZERO)
                .cashAdvanceFee(BigDecimal.ZERO)
                .carryForwardBalance(new BigDecimal("1020.00"))
                .statementStatus(Statement.StatementStatus.GENERATED)
                .build();

        RetrieveStatementResponseDTO result =
                statementMapper.toRetrieveResponseDTO(statement, List.of());

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
