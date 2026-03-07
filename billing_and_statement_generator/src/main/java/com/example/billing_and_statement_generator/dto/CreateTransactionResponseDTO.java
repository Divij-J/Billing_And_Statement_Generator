package com.example.billing_and_statement_generator.dto;

import com.example.billing_and_statement_generator.entity.Transaction.*;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTransactionResponseDTO {
    private UUID transactionId;
    private UUID cardId;
    private UUID cycleId;
    private LocalDate transactionDate;
    private transactionType type;
    private BigDecimal amount;
    private String merchantName;
    private Status status;
}