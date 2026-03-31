package com.example.billing_and_statement_generator.dto.transaction;

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
    @NotNull(message = "Card ID is required")
    private UUID cardId;

    @NotNull(message = "Transaction Date is required")
    @PastOrPresent
    private LocalDate transactionDate;

    @NotNull(message = "Amount is required")
    @DecimalMin("0.01") //transaction is at least $0.01
    @Digits(integer = 12, fraction = 2)
    private BigDecimal amount;

    @NotBlank(message = "Merchant name is required")
    @Size(max = 140)
    private String merchantName;

    @NotNull(message = "Transaction Type is required")
    private transactionType transactionType;
}