package com.example.billing_and_statement_generator.dto.statement;

import com.example.billing_and_statement_generator.dto.payment.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetrieveStatementResponseDTO {

    private String statementId;
    private String cycleId;
    private String cardId;
    private String statementDate;
    private String dueDate;
    private String billingStartDate;
    private String billingEndDate;
    private String statementBalance;
    private String remainingStatementBalance;
    private String minimumDue;
    private String totalInterest;
    private String totalOutstanding;
    private String totalFeeApplied;
    private String cashAdvanceFee;
    private String carryForwardBalance;
    private String amountPaid;
    private String availableCredit;
    private String statementStatus;

    // Transaction list for the billing cycle
    private List<CreateTransactionResponseDTO> transactions;

    // Payment list for the billing cycle
    private List<RetrievePaymentHistoryDTO> payments;
}