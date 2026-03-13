package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Statement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class StatementMapper {
    public Statement toEntity(
            Card card,
            BillingCycle billingCycle,
            BigDecimal previousBalance,
            BigDecimal newBalance,
            BigDecimal minimumDue,
            BigDecimal totalInterest,
            BigDecimal totalOutstanding,
            BigDecimal totalFeeApplied,
            BigDecimal cashAdvanceFee,
            BigDecimal carryForwardBalance
    ) {
        return Statement.builder()
                //Server generates statementId
                .statementId(UUID.randomUUID())
                .card(card)
                .billingCycle(billingCycle)

                //Statement date = today
                .statementDate(LocalDate.now())

                //Due date = billing cycle's due date
                .dueDate(billingCycle.getDue_date())

                //All calculated balance fields from service layer
                .previousBalance(previousBalance)
                .newBalance(newBalance)
                .minimumDue(minimumDue)
                .totalInterest(totalInterest)
                .totalOutstanding(totalOutstanding)
                .totalFeeApplied(totalFeeApplied)
                .cashAdvanceFee(cashAdvanceFee)
                .carryForwardBalance(carryForwardBalance)

                //Server sets initial status to GENERATED
                .statementStatus(Statement.StatementStatus.GENERATED)
                .build();
    }
    public GenerateStatementResponseDTO toGenerateResponseDTO(Statement statement) {
        return GenerateStatementResponseDTO.builder()
                .statementId(statement.getStatementId().toString())
                .cardId(statement.getCard().getCard_id().toString())
                .cycleId(statement.getBillingCycle().getCycle_id().toString())
                .statementStatus(statement.getStatementStatus().toString())
                .message("Statement generated successfully")
                .build();
    }
    public RetrieveStatementResponseDTO toRetrieveResponseDTO(Statement statement) {
        return RetrieveStatementResponseDTO.builder()
                .statementId(statement.getStatementId().toString())
                .cycleId(statement.getBillingCycle().getCycle_id().toString())
                .cardId(statement.getCard().getCard_id().toString())
                .statementDate(statement.getStatementDate().toString())
                .dueDate(statement.getDueDate().toString())
                .previousBalance(statement.getPreviousBalance().toString())
                .newBalance(statement.getNewBalance().toString())
                .minimumDue(statement.getMinimumDue().toString())
                .totalInterest(statement.getTotalInterest().toString())
                .totalOutstanding(statement.getTotalOutstanding().toString())
                .totalFeeApplied(statement.getTotalFeeApplied().toString())
                .cashAdvanceFee(statement.getCashAdvanceFee().toString())
                .carryForwardBalance(statement.getCarryForwardBalance().toString())
                .statementStatus(statement.getStatementStatus().toString())
                .build();
    }
}