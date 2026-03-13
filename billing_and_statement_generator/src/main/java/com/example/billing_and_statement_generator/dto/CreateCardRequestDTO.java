package com.example.billing_and_statement_generator.dto;

import com.example.billing_and_statement_generator.entity.Card.CardType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCardRequestDTO {
    @NotNull
    private UUID customerId;

    @NotBlank
    @Size(min = 12, max = 19)
    @Pattern(regexp = "^[0-9]+$", message = "cardNumber must be digits only")
    private String cardNumber;

    @NotNull
    private CardType cardType;

    @NotBlank
    private String cardHolderName;

    @NotNull
    @PastOrPresent
    private LocalDate cardIssueDate;

    @NotNull
    @Future
    private LocalDate expiryDate;

    @NotNull
    @DecimalMin("100.00")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal creditLimit;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 5, fraction = 2)
    private BigDecimal annualInterestRate;

    @NotNull
    private LocalDate billingCycleDate;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 5, fraction = 2)
    private BigDecimal lateFeeAmount;

    @NotNull
    @DecimalMin("0.00") @DecimalMax("10.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal cashAdvanceFeeRate;

    @NotBlank
    @Size(min = 3, max = 4)
    @Pattern(regexp = "^[0-9]+$", message = "securityCode must be digits only")
    private String securityCode;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 5, fraction = 2)
    private BigDecimal annualMembershipFee;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal cashAdvanceLimit;
}
