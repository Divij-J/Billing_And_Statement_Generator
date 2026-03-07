package com.example.billing_and_statement_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateStatementResponseDTO {

    private String statementId;
    private String cardId;
    private String cycleId;
    private String statementStatus;
    private String message;

}