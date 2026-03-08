package com.example.billing_and_statement_generator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateStatementRequestDTO {

    @NotBlank(message = "Card ID is required")
    private String cardId;

    @NotBlank(message = "Cycle ID is required")
    private String cycleId;
}