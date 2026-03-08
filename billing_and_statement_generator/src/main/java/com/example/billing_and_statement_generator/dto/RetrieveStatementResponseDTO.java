package com.example.billing_and_statement_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String previousBalance;
    private String newBalance;
    private String minimumDue;
    private String totalInterest;
    private String totalOutstanding;
    private String totalFeeApplied;
    private String cashAdvanceFee;
    private String carryForwardBalance;
    private String statementStatus;
}