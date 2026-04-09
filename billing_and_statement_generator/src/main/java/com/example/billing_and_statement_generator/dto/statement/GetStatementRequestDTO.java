package com.example.billing_and_statement_generator.dto.statement;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetStatementRequestDTO {

    @NotBlank(message = "Statement Id is required")
    private String statementId;
}
