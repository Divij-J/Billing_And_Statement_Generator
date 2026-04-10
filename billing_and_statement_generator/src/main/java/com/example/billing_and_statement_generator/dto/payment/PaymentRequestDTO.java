package com.example.billing_and_statement_generator.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDTO {

    @NotBlank(message = "Card ID is required")
    private String cardId;

    @NotBlank(message = "Amount paid is required")
    @Pattern(
            regexp = "^\\d+(\\.\\d{1,2})?$",
            message = "Amount must be a valid positive number with up to 2 decimal places"
    )
    private String amountPaid;

    // paymentType is now server-determined (FULL / PARTIAL / MINIMUM) based on amount vs billing cycle outstanding

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // BANK_TRANSFER, CHECK, ONLINE
}