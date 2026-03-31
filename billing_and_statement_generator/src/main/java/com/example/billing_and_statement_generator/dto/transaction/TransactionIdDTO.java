package com.example.billing_and_statement_generator.dto.transaction;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionIdDTO {
    @NotNull(message = "Transaction ID is required")
    UUID transactionId;
}
