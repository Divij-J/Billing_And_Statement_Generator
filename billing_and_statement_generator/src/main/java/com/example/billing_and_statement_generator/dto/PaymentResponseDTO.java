package com.example.billing_and_statement_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {

    private String paymentId;
    private String cycleId;
    private String cardId;
    private String amountPaid;
    private String paymentDate;
    private String paymentType;
    private String paymentStatus;

}