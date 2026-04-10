package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.payment.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.statement.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Statement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StatementMapper {

    private final TransactionMapper transactionMapper;

    public Statement toEntity(
            Card card,
            BillingCycle billingCycle,
            BigDecimal statementBalance,
            BigDecimal remainingStatementBalance,
            BigDecimal minimumDue,
            BigDecimal totalInterest,
            BigDecimal totalOutstanding,
            BigDecimal totalFeeApplied,
            BigDecimal cashAdvanceFee,
            BigDecimal carryForwardBalance
    ) {
        return Statement.builder()
                .statementId(UUID.randomUUID())
                .card(card)
                .billingCycle(billingCycle)
                .statementDate(java.time.LocalDate.now())
                .dueDate(billingCycle.getDueDate())
                .billingStartDate(billingCycle.getCycleStartDate())
                .billingEndDate(billingCycle.getCycleEndDate())
                .statementBalance(statementBalance)
                .remainingStatementBalance(remainingStatementBalance)
                .minimumDue(minimumDue)
                .totalInterest(totalInterest)
                .totalOutstanding(totalOutstanding)
                .totalFeeApplied(totalFeeApplied)
                .cashAdvanceFee(cashAdvanceFee)
                .carryForwardBalance(carryForwardBalance)
                .statementStatus(Statement.StatementStatus.GENERATED)
                .build();
    }

    public GenerateStatementResponseDTO toGenerateResponseDTO(Statement statement) {
        return GenerateStatementResponseDTO.builder()
                .statementId(statement.getStatementId().toString())
                .cardId(statement.getCard().getCardId().toString())
                .cycleId(statement.getBillingCycle().getCycleId().toString())
                .statementStatus(statement.getStatementStatus().toString())
                .message("Statement generated successfully")
                .build();
    }

    public RetrieveStatementResponseDTO toRetrieveResponseDTO(
            Statement statement,
            List<CreateTransactionResponseDTO> transactions,
            List<RetrievePaymentHistoryDTO> payments,
            BigDecimal amountPaid) {
        return RetrieveStatementResponseDTO.builder()
                .statementId(statement.getStatementId().toString())
                .cycleId(statement.getBillingCycle().getCycleId().toString())
                .cardId(statement.getCard().getCardId().toString())
                .statementDate(statement.getStatementDate().toString())
                .dueDate(statement.getDueDate().toString())
                .billingStartDate(statement.getBillingStartDate().toString())
                .billingEndDate(statement.getBillingEndDate().toString())
                .statementBalance(statement.getStatementBalance().toString())
                .remainingStatementBalance(statement.getRemainingStatementBalance().toString())
                .minimumDue(statement.getMinimumDue().toString())
                .totalInterest(statement.getTotalInterest().toString())
                .totalOutstanding(statement.getTotalOutstanding().toString())
                .totalFeeApplied(statement.getTotalFeeApplied().toString())
                .cashAdvanceFee(statement.getCashAdvanceFee().toString())
                .carryForwardBalance(statement.getCarryForwardBalance().toString())
                .statementStatus(statement.getStatementStatus().toString())
                .availableCredit(statement.getCard().getAvailableCredit() != null
                        ? statement.getCard().getAvailableCredit().toString() : "0.00")
                .amountPaid(amountPaid != null ? amountPaid.toString() : "0.00")
                .transactions(transactions)
                .payments(payments)
                .build();
    }
}
