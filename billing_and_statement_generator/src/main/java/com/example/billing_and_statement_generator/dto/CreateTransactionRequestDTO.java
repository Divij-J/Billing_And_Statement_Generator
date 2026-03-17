package com.example.billing_and_statement_generator.dto;

import com.example.billing_and_statement_generator.entity.Transaction.*;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTransactionRequestDTO {
    @NotNull
    private UUID cardId;

    @NotNull
    @PastOrPresent
    private LocalDate transactionDate;

    @NotNull
    @DecimalMin("0.01") //transaction is at least $0.01
    @Digits(integer = 12, fraction = 2)
    private BigDecimal amount;

    @NotBlank
    @Size(max = 140)
    private String merchantName;

    @NotNull
    private transactionType type;
}