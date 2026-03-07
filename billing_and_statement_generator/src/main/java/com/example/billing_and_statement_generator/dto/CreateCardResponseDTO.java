package com.example.billing_and_statement_generator.dto;

import com.example.billing_and_statement_generator.entity.Card;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCardResponseDTO {
    private UUID customerId;
    private UUID cardId;
    private String cardNumber;
    private Card.CardType cardType;
    private String cardHolderName;
    private LocalDate cardIssueDate;
    private LocalDate expiryDate;
    private boolean isActive;
    private BigDecimal creditLimit;
    private BigDecimal annualInterestRate;
    private LocalDate billingCycleDate;
    private BigDecimal lateFeeAmount;
    private BigDecimal cashAdvanceFeeRate;
    private String securityCode;
    private BigDecimal annualMembershipFee;
    private BigDecimal cashAdvanceLimit;
}
